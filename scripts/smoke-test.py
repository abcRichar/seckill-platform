"""
端到端冒烟测试（Python 标准库）。

覆盖：
- 公开商品/秒杀接口
- 管理员登录、看板、商品/活动/订单/用户/角色/日志
- 用户注册、秒杀令牌、Lua 下单、异步结果轮询、支付幂等、重复购买
- 第二个用户下单后取消，验证库存回补
- 权限校验（用户 token 访问管理端 403）
- Excel 导出文件头

用法：
  python scripts/smoke-test.py
"""
import json
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import quote

# Windows 控制台默认 GBK，强制 UTF-8 避免中文乱码
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

BASE = "http://localhost:9090/api"
PASS = 0
FAIL = 0


def check(name, condition, detail=""):
    global PASS, FAIL
    if condition:
        PASS += 1
        print(f"[PASS] {name}")
    else:
        FAIL += 1
        print(f"[FAIL] {name} {detail}")


def request(method, path, token=None, data=None, raw=False):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(BASE + quote(path, safe="/?&=:%"), data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            content = resp.read()
            if raw:
                return content, resp.headers
            return json.loads(content.decode("utf-8"))
    except urllib.error.HTTPError as e:
        content = e.read()
        if raw:
            return content, e.headers
        try:
            return json.loads(content.decode("utf-8"))
        except Exception:
            return {"code": e.code, "message": str(e)}


def login(username, password, admin=False):
    url = "/auth/admin/login" if admin else "/auth/login"
    res = request("POST", url, data={"username": username, "password": password})
    return res


def wait_order(token, order_no, timeout=20):
    deadline = time.time() + timeout
    last = None
    while time.time() < deadline:
        res = request("GET", f"/mall/seckill/result?orderNo={order_no}", token=token)
        if res.get("code") == 200:
            data = res["data"]
            last = data
            if data.get("status") in ("SUCCESS", "FAILED"):
                return data
        time.sleep(0.5)
    return last or {"status": "TIMEOUT"}


def register(prefix):
    username = f"{prefix}{int(time.time() * 1000) % 100000000}"
    res = request(
        "POST",
        "/auth/register",
        data={"username": username, "password": "123456", "nickname": prefix},
    )
    return res, username


def main():
    print("=" * 60)
    print("秒杀平台端到端冒烟测试")
    print("=" * 60)

    # 1. 公开接口
    res = request("GET", "/mall/categories")
    check("分类列表", res.get("code") == 200 and len(res.get("data") or []) >= 4, res)
    categories = res.get("data") or []

    res = request("GET", "/mall/products?page=1&size=8&sort=sales")
    products = res.get("data") or {}
    check("商品分页", res.get("code") == 200 and len(products.get("records") or []) > 0, res)

    first_product_id = products["records"][0]["id"]
    t0 = time.time()
    res1 = request("GET", f"/mall/products/{first_product_id}")
    first_ms = (time.time() - t0) * 1000
    t0 = time.time()
    res2 = request("GET", f"/mall/products/{first_product_id}")
    second_ms = (time.time() - t0) * 1000
    check("商品详情 + 缓存", res1.get("code") == 200 and res2.get("code") == 200,
          f"first={first_ms:.1f}ms second={second_ms:.1f}ms")
    print(f"       缓存对比：首次 {first_ms:.1f}ms / 二次 {second_ms:.1f}ms")

    res = request("GET", "/mall/seckill/list")
    activities = res.get("data") or []
    check("秒杀活动列表", res.get("code") == 200 and len(activities) > 0, res)

    ongoing = next((a for a in activities if a.get("state") == "ONGOING" and a.get("stock", 0) > 0), None)
    check("存在进行中且有库存的活动", ongoing is not None)

    # 2. 管理端
    res = login("admin", "123456", admin=True)
    admin_token = (res.get("data") or {}).get("token")
    check("管理员登录", res.get("code") == 200 and bool(admin_token), res)
    check("管理员权限含 *", "*" in ((res.get("data") or {}).get("permissions") or []), res)

    res = request("GET", "/admin/auth/me", token=admin_token)
    check("管理员 /me", res.get("code") == 200 and res["data"]["username"] == "admin", res)

    res = request("GET", "/admin/dashboard", token=admin_token)
    dash = res.get("data") or {}
    check("数据看板", res.get("code") == 200 and "todayOrders" in dash and "trend" in dash, res)
    print(f"       看板：今日订单={dash.get('todayOrders')} 今日成交={dash.get('todaySales')} "
          f"用户={dash.get('userCount')} 活跃活动={dash.get('activeActivities')}")

    res = request("GET", "/admin/products?page=1&size=5", token=admin_token)
    check("管理端商品分页", res.get("code") == 200 and res["data"]["total"] > 0, res)

    res = request("GET", "/admin/seckill?page=1&size=5", token=admin_token)
    check("管理端活动分页", res.get("code") == 200 and res["data"]["total"] > 0, res)

    res = request("GET", "/admin/orders?page=1&size=5", token=admin_token)
    check("管理端订单分页", res.get("code") == 200 and res["data"]["total"] > 0, res)

    res = request("GET", "/admin/users?page=1&size=5", token=admin_token)
    check("管理端用户分页", res.get("code") == 200 and res["data"]["total"] > 0, res)

    res = request("GET", "/admin/roles", token=admin_token)
    check("管理端角色列表", res.get("code") == 200 and len(res["data"]) >= 2, res)

    res = request("GET", "/admin/roles/menus", token=admin_token)
    check("管理端权限点", res.get("code") == 200 and len(res["data"]) >= 10, res)

    res = request("GET", "/admin/logs?page=1&size=5", token=admin_token)
    check("管理端操作日志", res.get("code") == 200, res)

    content, headers = request("GET", "/admin/orders/export", token=admin_token, raw=True)
    check("订单 Excel 导出", content[:2] == b"PK", f"header={content[:8]!r}")

    # 3. 用户秒杀完整链路
    res, smoke_user = register("smoke")
    user_token = (res.get("data") or {}).get("token")
    check("用户注册并自动登录", res.get("code") == 200 and bool(user_token), res)

    if ongoing and user_token:
        path = ongoing.get("seckillPath")
        detail = request("GET", f"/mall/seckill/{ongoing['id']}")
        path = (detail.get("data") or {}).get("seckillPath") or path
        check("秒杀详情含隐藏 path", detail.get("code") == 200 and bool(path), detail)

        res = request("GET", f"/mall/seckill/{ongoing['id']}/mine", token=user_token)
        check("未参与状态", res.get("code") == 200 and res["data"]["purchased"] is False, res)

        res = request("POST", f"/mall/seckill/{ongoing['id']}/token?path={path}", token=user_token)
        seckill_token = (res.get("data") or {}).get("token")
        check("获取一次性令牌", res.get("code") == 200 and bool(seckill_token), res)

        before = request("GET", f"/mall/seckill/{ongoing['id']}")
        before_stock = (before.get("data") or {}).get("stock")

        res = request(
            "POST",
            "/mall/seckill/order",
            token=user_token,
            data={
                "activityId": ongoing["id"],
                "token": seckill_token,
                "path": path,
                "quantity": 1,
            },
        )
        order_no = (res.get("data") or {}).get("orderNo")
        check("Lua 预扣并写入 Stream", res.get("code") == 200 and bool(order_no), res)

        result = wait_order(user_token, order_no)
        check("异步订单落库", result.get("status") == "SUCCESS" and result.get("orderId"), result)
        order_id = result.get("orderId")

        res = request("GET", f"/mall/seckill/{ongoing['id']}/mine", token=user_token)
        check("已参与状态", res.get("code") == 200 and res["data"]["purchased"] is True, res)

        res = request("POST", f"/mall/seckill/{ongoing['id']}/token?path={path}", token=user_token)
        check("重复购买被拦截", res.get("code") == 409 and res.get("bizCode") == 1002, res)

        after = request("GET", f"/mall/seckill/{ongoing['id']}")
        after_stock = (after.get("data") or {}).get("stock")
        check("Redis 库存 -1", before_stock is not None and after_stock == before_stock - 1,
              f"before={before_stock} after={after_stock}")

        if order_id:
            res = request("GET", f"/mall/orders/{order_id}", token=user_token)
            check("用户订单详情", res.get("code") == 200 and res["data"]["orderNo"] == order_no, res)

            res = request("POST", f"/mall/orders/{order_id}/pay", token=user_token)
            check("模拟支付", res.get("code") == 200 and res["data"]["status"] == 1, res)

            res = request("POST", f"/mall/orders/{order_id}/pay", token=user_token)
            check("重复支付幂等", res.get("code") == 200 and res["data"]["status"] == 1, res)

            res = request("POST", f"/mall/orders/{order_id}/cancel", token=user_token)
            check("已支付订单不可取消", res.get("code") == 400, res)

    # 4. 取消订单回补库存
    res, cancel_user = register("cancel")
    cancel_token = (res.get("data") or {}).get("token")
    ongoing2 = next(
        (a for a in activities
         if a.get("state") == "ONGOING" and a.get("stock", 0) > 0 and a.get("id") != (ongoing or {}).get("id")),
        None,
    )
    if ongoing2 and cancel_token:
        detail = request("GET", f"/mall/seckill/{ongoing2['id']}")
        path2 = (detail.get("data") or {}).get("seckillPath")
        before2 = (detail.get("data") or {}).get("stock")
        token_res = request("POST", f"/mall/seckill/{ongoing2['id']}/token?path={path2}", token=cancel_token)
        token2 = (token_res.get("data") or {}).get("token")
        order_res = request(
            "POST",
            "/mall/seckill/order",
            token=cancel_token,
            data={"activityId": ongoing2["id"], "token": token2, "path": path2, "quantity": 1},
        )
        order_no2 = (order_res.get("data") or {}).get("orderNo")
        result2 = wait_order(cancel_token, order_no2) if order_no2 else {}
        order_id2 = result2.get("orderId")
        check("第二用户下单", result2.get("status") == "SUCCESS", result2)

        if order_id2:
            res = request("POST", f"/mall/orders/{order_id2}/cancel", token=cancel_token)
            check("取消待支付订单", res.get("code") == 200, res)
            time.sleep(0.5)
            after2 = request("GET", f"/mall/seckill/{ongoing2['id']}")
            stock2 = (after2.get("data") or {}).get("stock")
            check("取消后 Redis 库存回补", stock2 == before2, f"before={before2} after={stock2}")

    # 5. 鉴权
    res = request("GET", "/mall/orders")
    check("未登录访问订单返回 401", res.get("code") == 401, res)

    if user_token:
        res = request("GET", "/admin/dashboard", token=user_token)
        check("普通用户访问管理端返回 403", res.get("code") == 403, res)

    print("-" * 60)
    print(f"结果：PASS={PASS}  FAIL={FAIL}")
    print("-" * 60)
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
