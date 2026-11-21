"""
管理端写接口验证：上传、商品、秒杀活动、发布/下架、角色、用户状态。

会创建测试数据，运行后可在最后重置数据库（见 README）。
"""
import json
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import quote
import uuid
from datetime import datetime, timedelta

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


def upload(token, file_path):
    boundary = "----seckill" + uuid.uuid4().hex
    with open(file_path, "rb") as f:
        content = f.read()
    head = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="test.svg"\r\n'
        "Content-Type: image/svg+xml\r\n\r\n"
    ).encode()
    body = head + content + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(
        BASE + "/admin/upload",
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def main():
    print("=" * 60)
    print("管理端写接口验证")
    print("=" * 60)

    res = request("POST", "/auth/admin/login", data={"username": "admin", "password": "123456"})
    token = res["data"]["token"]
    check("管理员登录", res.get("code") == 200)

    res = upload(token, "E:/seckill-platform/uploads/seed/phone.svg")
    cover = (res.get("data") or {}).get("url")
    check("图片上传", res.get("code") == 200 and cover and cover.startswith("/uploads/"), res)

    product_name = f"冒烟测试手机{int(time.time()) % 100000}"
    res = request(
        "POST",
        "/admin/products",
        token=token,
        data={
            "name": product_name,
            "subtitle": "管理员写接口验证",
            "categoryId": 1,
            "price": 999.00,
            "originalPrice": 1299.00,
            "stock": 10,
            "status": 1,
            "cover": cover,
            "images": cover,
            "description": "仅用于测试",
        },
    )
    check("新增商品", res.get("code") == 200, res)

    res = request("GET", f"/admin/products?page=1&size=10&keyword={product_name}", token=token)
    records = (res.get("data") or {}).get("records") or []
    product_id = records[0]["id"] if records else None
    check("查询新商品", product_id is not None, res)
    if not product_id:
        return finish()

    res = request("PUT", f"/admin/products/{product_id}/status?status=0", token=token)
    check("商品下架", res.get("code") == 200, res)
    res = request("PUT", f"/admin/products/{product_id}/status?status=1", token=token)
    check("商品上架", res.get("code") == 200, res)

    now = datetime.now()
    title = f"冒烟测试秒杀{int(time.time()) % 100000}"
    res = request(
        "POST",
        "/admin/seckill",
        token=token,
        data={
            "productId": product_id,
            "title": title,
            "seckillPrice": 499.00,
            "stock": 5,
            "limitPerUser": 1,
            "startTime": (now - timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S"),
            "endTime": (now + timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S"),
        },
    )
    check("新增秒杀活动", res.get("code") == 200, res)

    res = request("GET", f"/admin/seckill?page=1&size=10&keyword={title}", token=token)
    activities = (res.get("data") or {}).get("records") or []
    activity_id = activities[0]["id"] if activities else None
    check("查询新活动", activity_id is not None, res)

    if activity_id:
        res = request("PUT", f"/admin/seckill/{activity_id}/publish", token=token)
        check("活动上架", res.get("code") == 200, res)

        res = request("GET", f"/mall/seckill/{activity_id}")
        mall = res.get("data") or {}
        check("前台可见活动", res.get("code") == 200 and mall.get("state") == "ONGOING", res)
        check("前台库存已预热", mall.get("stock") == 5, mall)
        check("隐藏链接已生成", bool(mall.get("seckillPath")), mall)

        res = request("PUT", f"/admin/seckill/{activity_id}/stop", token=token)
        check("活动下架", res.get("code") == 200, res)

        res = request("DELETE", f"/admin/seckill/{activity_id}", token=token)
        check("删除活动", res.get("code") == 200, res)

    res = request("DELETE", f"/admin/products/{product_id}", token=token)
    check("删除商品", res.get("code") == 200, res)

    # 角色 CRUD
    role_code = f"SMOKE_{int(time.time()) % 100000}"
    res = request(
        "POST",
        "/admin/roles",
        token=token,
        data={"roleName": "冒烟角色", "roleCode": role_code, "remark": "测试"},
    )
    check("新增角色", res.get("code") == 200, res)
    res = request("GET", "/admin/roles", token=token)
    role = next((r for r in (res.get("data") or []) if r.get("roleCode") == role_code), None)
    check("查询新角色", role is not None, res)
    if role:
        res = request("PUT", f"/admin/roles/{role['id']}/menus", token=token, data={"menuIds": [2, 5]})
        check("分配角色权限", res.get("code") == 200, res)
        res = request("GET", f"/admin/roles/{role['id']}/menus", token=token)
        check("权限回读", res.get("code") == 200 and sorted(res.get("data") or []) == [2, 5], res)
        res = request("DELETE", f"/admin/roles/{role['id']}", token=token)
        check("删除角色", res.get("code") == 200, res)

    # 用户状态 / 重置密码
    username = f"admintest{int(time.time()) % 100000}"
    res = request("POST", "/auth/register", data={"username": username, "password": "123456"})
    user_id = (res.get("data") or {}).get("userId")
    check("注册测试用户", res.get("code") == 200 and user_id, res)
    if user_id:
        res = request("PUT", f"/admin/users/{user_id}/status?status=0", token=token)
        check("禁用用户", res.get("code") == 200, res)
        res = request("POST", "/auth/login", data={"username": username, "password": "123456"})
        check("禁用后无法登录", res.get("code") == 403, res)
        request("PUT", f"/admin/users/{user_id}/status?status=1", token=token)
        res = request("PUT", f"/admin/users/{user_id}/reset-password?password=654321", token=token)
        check("重置密码", res.get("code") == 200, res)
        res = request("POST", "/auth/login", data={"username": username, "password": "654321"})
        check("新密码可登录", res.get("code") == 200, res)
        res = request("PUT", f"/admin/users/{user_id}/roles", token=token, data=[2])
        check("分配用户角色", res.get("code") == 200, res)

    res = request("GET", "/admin/logs?page=1&size=50", token=token)
    logs = (res.get("data") or {}).get("records") or []
    actions = {log.get("action") for log in logs}
    check("操作日志已记录", {"新增商品", "新增活动", "上架活动", "新增角色"} <= actions,
          f"actions={sorted(a for a in actions if a)}")

    return finish()


def finish():
    print("-" * 60)
    print(f"结果：PASS={PASS}  FAIL={FAIL}")
    print("-" * 60)
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
