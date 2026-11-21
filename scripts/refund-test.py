"""
退款流程端到端测试：
A. 用户申请 -> 管理员通过 -> 订单已退款
B. 用户申请 -> 管理员拒绝 -> 用户重新申请 -> 通过
C. 管理员直接退款
D. 退款不回补库存、重复操作被拦截

会创建临时用户和订单，跑完可重置演示库。
"""
import json
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import quote

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

BASE = "http://localhost:9090/api"
PASS = 0
FAIL = 0


def check(name, cond, detail=""):
    global PASS, FAIL
    if cond:
        PASS += 1
        print(f"[PASS] {name}")
    else:
        FAIL += 1
        print(f"[FAIL] {name} {detail}")


def request(method, path, token=None, data=None):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(data, ensure_ascii=False).encode()
    req = urllib.request.Request(BASE + quote(path, safe="/?&=:%"), data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode())
        except Exception:
            return {"code": e.code, "message": str(e)}


def login(username, password, admin=False):
    url = "/auth/admin/login" if admin else "/auth/login"
    return request("POST", url, data={"username": username, "password": password})


def register(prefix, index):
    username = f"{prefix}{int(time.time()) % 100000000}{index}"
    res = request("POST", "/auth/register",
                  data={"username": username, "password": "123456", "nickname": prefix})
    return res, username


def wait_order(token, order_no, timeout=20):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        res = request("GET", f"/mall/seckill/result?orderNo={order_no}", token=token)
        if res.get("code") == 200:
            last = res["data"]
            if last.get("status") in ("SUCCESS", "FAILED"):
                return last
        time.sleep(0.4)
    return last or {"status": "TIMEOUT"}


def buy_and_pay(token, activity, path):
    before = request("GET", f"/mall/seckill/{activity}")
    stock_before = (before.get("data") or {}).get("stock")
    token_res = request("POST", f"/mall/seckill/{activity}/token?path={path}", token=token)
    seckill_token = (token_res.get("data") or {}).get("token")
    order_res = request("POST", "/mall/seckill/order", token=token,
                        data={"activityId": activity, "token": seckill_token, "path": path, "quantity": 1})
    order_no = (order_res.get("data") or {}).get("orderNo")
    result = wait_order(token, order_no) if order_no else {}
    order_id = result.get("orderId")
    if order_id:
        pay = request("POST", f"/mall/orders/{order_id}/pay", token=token)
        return order_id, order_no, stock_before, pay
    return None, order_no, stock_before, result


def find_refund(admin_token, keyword=None, status=None):
    params = "?page=1&size=50"
    if keyword:
        params += f"&keyword={quote(keyword)}"
    if status is not None:
        params += f"&status={status}"
    res = request("GET", "/admin/refunds" + params, token=admin_token)
    return (res.get("data") or {}).get("records") or []


def main():
    print("=" * 60)
    print("退款流程端到端测试")
    print("=" * 60)

    admin = login("admin", "123456", admin=True)
    admin_token = (admin.get("data") or {}).get("token")
    check("管理员登录", admin.get("code") == 200 and admin_token, admin)

    detail = request("GET", "/mall/seckill/1")
    activity = detail.get("data") or {}
    path = activity.get("seckillPath")
    check("活动进行中且拿到隐藏链接", activity.get("state") == "ONGOING" and path, detail)

    # ---------- A. 申请 -> 通过 ----------
    reg, username = register("rf", 1)
    token_a = (reg.get("data") or {}).get("token")
    check("注册用户A", reg.get("code") == 200 and token_a, reg)
    order_a, no_a, stock_before, pay_a = buy_and_pay(token_a, 1, path)
    check("用户A下单并支付", order_a is not None and pay_a.get("code") == 200, f"order={order_a} pay={pay_a}")

    res = request("POST", f"/mall/orders/{order_a}/refund", token=token_a,
                  data={"reason": "临时不需要了", "description": "测试申请退款"})
    check("用户申请退款", res.get("code") == 200 and res["data"]["status"] == 0, res)
    refund_a_id = (res.get("data") or {}).get("id")

    order = request("GET", f"/mall/orders/{order_a}", token=token_a).get("data") or {}
    check("订单进入退款审核中", order.get("refundStatus") == 1 and order.get("status") == 1, order)

    res = request("POST", f"/mall/orders/{order_a}/refund", token=token_a,
                  data={"reason": "重复申请"})
    check("重复申请被拦截", res.get("code") == 400, res)

    summary = request("GET", "/admin/refunds/summary", token=admin_token)
    check("退款统计可查询", summary.get("code") == 200 and summary["data"]["pending"] >= 1, summary)

    res = request("POST", f"/admin/refunds/{refund_a_id}/approve", token=admin_token,
                  data={"remark": "同意退款，原路退回"})
    check("管理员审核通过", res.get("code") == 200 and res["data"]["status"] == 1, res)

    order = request("GET", f"/mall/orders/{order_a}", token=token_a).get("data") or {}
    check("订单变为已退款", order.get("status") == 4 and order.get("refundStatus") == 2, order)

    refund = request("GET", f"/mall/orders/{order_a}/refund", token=token_a).get("data") or {}
    check("退款单审核信息", refund.get("status") == 1 and refund.get("auditName") == "admin", refund)

    res = request("POST", f"/mall/orders/{order_a}/pay", token=token_a)
    check("已退款订单不能支付", res.get("code") == 400, res)

    res = request("POST", f"/mall/orders/{order_a}/refund", token=token_a,
                  data={"reason": "再次申请"})
    check("已退款订单不能再次申请", res.get("code") == 400, res)

    after = request("GET", f"/mall/seckill/1")
    stock_after = (after.get("data") or {}).get("stock")
    check("退款不回补库存", stock_after == stock_before - 1, f"before={stock_before} after={stock_after}")

    # ---------- B. 申请 -> 拒绝 -> 重新申请 -> 通过 ----------
    reg, _ = register("rj", 2)
    token_b = (reg.get("data") or {}).get("token")
    order_b, no_b, _, pay_b = buy_and_pay(token_b, 1, path)
    check("用户B下单并支付", order_b is not None and pay_b.get("code") == 200, f"{order_b} {pay_b}")

    res = request("POST", f"/mall/orders/{order_b}/refund", token=token_b,
                  data={"reason": "商品不符合预期", "description": "先申请一次"})
    refund_b_id = (res.get("data") or {}).get("id")
    check("用户B申请退款", res.get("code") == 200, res)

    res = request("POST", f"/admin/refunds/{refund_b_id}/reject", token=admin_token,
                  data={"remark": "未满足退款条件"})
    check("管理员拒绝退款", res.get("code") == 200 and res["data"]["status"] == 2, res)

    order = request("GET", f"/mall/orders/{order_b}", token=token_b).get("data") or {}
    check("拒绝后订单仍为已支付", order.get("status") == 1 and order.get("refundStatus") == 3, order)

    res = request("POST", f"/mall/orders/{order_b}/refund", token=token_b,
                  data={"reason": "补充材料后重新申请", "description": "这次附上说明"})
    check("被拒后可重新申请", res.get("code") == 200 and res["data"]["status"] == 0, res)
    refund_b_id = (res.get("data") or {}).get("id")

    res = request("POST", f"/admin/refunds/{refund_b_id}/approve", token=admin_token,
                  data={"remark": "复核通过"})
    check("重新申请审核通过", res.get("code") == 200, res)
    order = request("GET", f"/mall/orders/{order_b}", token=token_b).get("data") or {}
    check("用户B订单已退款", order.get("status") == 4 and order.get("refundStatus") == 2, order)

    # ---------- C. 管理员直接退款 ----------
    reg, _ = register("rd", 3)
    token_c = (reg.get("data") or {}).get("token")
    order_c, no_c, _, pay_c = buy_and_pay(token_c, 1, path)
    check("用户C下单并支付", order_c is not None and pay_c.get("code") == 200, f"{order_c} {pay_c}")

    res = request("POST", f"/admin/orders/{order_c}/refund", token=admin_token,
                  data={"reason": "客服补偿退款", "description": "用户反馈物流异常"})
    check("管理员直接退款", res.get("code") == 200 and res["data"]["status"] == 1, res)
    order = request("GET", f"/mall/orders/{order_c}", token=token_c).get("data") or {}
    check("直接退款后订单已退款", order.get("status") == 4 and order.get("refundStatus") == 2, order)

    res = request("POST", f"/admin/orders/{order_c}/refund", token=admin_token,
                  data={"reason": "重复退款"})
    check("重复直接退款被拦截", res.get("code") == 400, res)

    # ---------- D. admin 退款列表 ----------
    records = find_refund(admin_token, status=1)
    check("退款列表可按状态筛选", len(records) >= 3, len(records))
    records = find_refund(admin_token, keyword=no_a)
    check("退款列表可按订单号搜索", any(r.get("orderNo") == no_a for r in records), records)

    print("-" * 60)
    print(f"结果：PASS={PASS}  FAIL={FAIL}")
    print("-" * 60)
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
