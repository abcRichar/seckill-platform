"""
验证 HTTP 状态码语义：
- 2xx 成功
- 4xx 参数/鉴权/业务冲突/限流
- 5xx 服务端异常（本脚本只验证可预期的 4xx）
"""
import json
import sys
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


def call(method, path, token=None, data=None):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(data, ensure_ascii=False).encode()
    req = urllib.request.Request(BASE + quote(path, safe="/?&=:%"), data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read()
            return resp.status, json.loads(raw.decode()) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw.decode())
        except Exception:
            return e.code, None


def login(username, password, admin=False):
    path = "/auth/admin/login" if admin else "/auth/login"
    return call("POST", path, data={"username": username, "password": password})


def main():
    print("=" * 60)
    print("HTTP 状态码语义验证")
    print("=" * 60)

    status, body = call("GET", "/mall/categories")
    check("成功 → HTTP 200 + code 200", status == 200 and body and body["code"] == 200, f"http={status} body={body}")

    status, body = call("GET", "/mall/orders")
    check("未登录 → HTTP 401 + code 401", status == 401 and body and body["code"] == 401, f"http={status} body={body}")

    status, body = login("alice", "123456")
    alice = (body or {}).get("data", {}).get("token")
    check("用户登录 → HTTP 200", status == 200 and body and body["code"] == 200 and alice, f"http={status}")

    status, body = call("GET", "/admin/dashboard", token=alice)
    check("普通用户访问管理端 → HTTP 403 + code 403", status == 403 and body and body["code"] == 403, f"http={status} body={body}")

    status, body = login("alice", "wrong-password")
    check("密码错误 → HTTP 400 + code 400", status == 400 and body and body["code"] == 400, f"http={status} body={body}")

    status, body = call("POST", "/auth/register", data={"username": "abc", "password": "123456"})
    check("参数校验失败 → HTTP 400 + code 400", status == 400 and body and body["code"] == 400, f"http={status} body={body}")

    status, body = call("GET", "/mall/products/999999")
    check("资源不存在 → HTTP 404 + code 404", status == 404 and body and body["code"] == 404, f"http={status} body={body}")

    status, body = call("GET", "/not-exist-api")
    check("未知接口 → HTTP 404", status == 404, f"http={status} body={body}")

    status, body = call("GET", "/auth/login")
    check("方法不支持 → HTTP 405", status == 405 and body and body["code"] == 405, f"http={status} body={body}")

    # 业务冲突：alice 已经买过活动 1，再取令牌 → 409 + 业务码 1002
    status, body = call("GET", "/mall/seckill/1")
    path = (body or {}).get("data", {}).get("seckillPath")
    status, body = call("POST", f"/mall/seckill/1/token?path={path}", token=alice)
    check("重复购买 → HTTP 409 + code 409 + bizCode 1002", status == 409 and body and body["code"] == 409 and body.get("bizCode") == 1002, f"http={status} body={body}")

    # 限流：用 bob 对已结束活动反复取令牌（不会产生任何数据）
    status, body = login("bob", "123456")
    bob = (body or {}).get("data", {}).get("token")
    results = []
    for i in range(4):
        st, bd = call("POST", "/mall/seckill/4/token?path=whatever", token=bob)
        results.append((st, (bd or {}).get("code"), (bd or {}).get("bizCode")))
    check("限流 → 前 3 次 409 + bizCode 1003，第 4 次 429",
          results[:3] == [(409, 409, 1003)] * 3 and results[3] == (429, 429, None),
          f"results={results}")

    print("-" * 60)
    print(f"结果：PASS={PASS}  FAIL={FAIL}")
    print("-" * 60)
    return 0 if FAIL == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
