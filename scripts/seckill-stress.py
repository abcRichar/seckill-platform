"""
秒杀并发验证脚本（仅用 Python 标准库，无需安装依赖）。

用法：
  python scripts/seckill-stress.py --activity 1 --users 30

流程：
  1. 读取活动详情，拿到隐藏 path
  2. 并发注册临时用户并登录
  3. 每个用户：取令牌 -> 下单 -> 轮询结果
  4. 汇总成功/失败/排队，并打印剩余库存

注意：
  - 活动必须是「进行中」且有库存
  - 临时用户名为 load{时间戳}_{i}，不会影响演示账号
  - 压测会消耗真实库存，结束后可在管理端「下架 -> 上架」重置 Redis 库存
"""
import argparse
import json
import time
import urllib.error
import urllib.request
from urllib.parse import quote
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE = "http://localhost:9090/api"


def request(method, path, token=None, data=None):
    body = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if data is not None:
        body = json.dumps(data, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(BASE + quote(path, safe="/?&=:%"), data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return json.loads(e.read().decode("utf-8"))
        except Exception:
            return {"code": e.code, "message": str(e)}
    except Exception as e:
        return {"code": -1, "message": str(e)}


def register_user(index):
    username = f"load{int(time.time())}_{index}"
    payload = {"username": username, "password": "123456", "nickname": f"压测用户{index}"}
    res = request("POST", "/auth/register", data=payload)
    if res.get("code") == 200:
        return res["data"]["token"], username
    # 用户名冲突时尝试登录
    res = request("POST", "/auth/login", data={"username": username, "password": "123456"})
    if res.get("code") == 200:
        return res["data"]["token"], username
    return None, username


def seckill_once(token, activity_id, path):
    token_res = request("POST", f"/mall/seckill/{activity_id}/token?path={path}", token=token)
    if token_res.get("code") != 200:
        return {"stage": "token", "message": token_res.get("message")}
    seckill_token = token_res["data"]["token"]

    order_res = request(
        "POST",
        "/mall/seckill/order",
        token=token,
        data={
            "activityId": activity_id,
            "token": seckill_token,
            "path": path,
            "quantity": 1,
        },
    )
    if order_res.get("code") != 200:
        return {"stage": "order", "message": order_res.get("message")}
    order_no = order_res["data"]["orderNo"]

    for _ in range(25):
        time.sleep(0.4)
        result = request("GET", f"/mall/seckill/result?orderNo={order_no}", token=token)
        if result.get("code") != 200:
            continue
        status = result["data"].get("status")
        if status in ("SUCCESS", "FAILED"):
            return {"stage": "done", "status": status, "message": result["data"].get("message")}
    return {"stage": "done", "status": "QUEUED", "message": "轮询超时，仍在排队"}


def main():
    global BASE
    parser = argparse.ArgumentParser()
    parser.add_argument("--activity", type=int, required=True, help="秒杀活动 ID")
    parser.add_argument("--users", type=int, default=20, help="并发用户数")
    parser.add_argument("--base", default=BASE, help="后端 API 地址")
    args = parser.parse_args()

    BASE = args.base.rstrip("/")

    detail = request("GET", f"/mall/seckill/{args.activity}")
    if detail.get("code") != 200:
        print("[ERROR] 活动不存在:", detail.get("message"))
        return
    activity = detail["data"]
    if activity.get("state") != "ONGOING":
        print(f"[ERROR] 活动当前状态为 {activity.get('state')}，请先上架并确保在活动时间内")
        return
    path = activity.get("seckillPath")
    if not path:
        print("[ERROR] 未拿到隐藏链接 seckillPath")
        return

    print(f"活动: {activity.get('title')}  当前库存: {activity.get('stock')}  并发: {args.users}")
    print("正在注册临时用户...")
    tokens = []
    with ThreadPoolExecutor(max_workers=min(args.users, 10)) as pool:
        futures = [pool.submit(register_user, i) for i in range(args.users)]
        for future in as_completed(futures):
            token, username = future.result()
            if token:
                tokens.append(token)
    print(f"可用账号: {len(tokens)}")

    stats = {"SUCCESS": 0, "FAILED": 0, "QUEUED": 0}
    reasons = {}
    print("开始并发抢购...")
    start = time.time()
    with ThreadPoolExecutor(max_workers=args.users) as pool:
        futures = [pool.submit(seckill_once, token, args.activity, path) for token in tokens]
        for future in as_completed(futures):
            result = future.result()
            if result.get("stage") == "done":
                stats[result["status"]] = stats.get(result["status"], 0) + 1
            else:
                stats["FAILED"] = stats.get("FAILED", 0) + 1
                reason = f"{result.get('stage')}: {result.get('message')}"
                reasons[reason] = reasons.get(reason, 0) + 1

    elapsed = time.time() - start
    print("-" * 52)
    print(f"耗时: {elapsed:.2f}s")
    print(f"成功: {stats.get('SUCCESS', 0)}  失败: {stats.get('FAILED', 0)}  排队: {stats.get('QUEUED', 0)}")
    for reason, count in sorted(reasons.items(), key=lambda x: -x[1]):
        print(f"  {count:>4}  {reason}")

    time.sleep(1)
    after = request("GET", f"/mall/seckill/{args.activity}")
    if after.get("code") == 200:
        print(f"剩余库存: {after['data'].get('stock')}（开始前 {activity.get('stock')}）")


if __name__ == "__main__":
    main()
