import asyncio
import aiohttp
import time
import random
import string
import csv
import json
from datetime import datetime

# BASE_URL = "https://esewaprod.sudarshan-uprety.com.np"
BASE_URL = "http://localhost:8080" # Uncomment for local testing

CONCURRENCY = 100  # Reduced for stability during logging
TOTAL_POSTS = 1000 # Total POST requests per endpoint
TOTAL_GETS = 500   # Total GET requests per endpoint

def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))

def truncate_text(text, max_length=100):
    if not text: return ""
    return (text[:max_length] + '..') if len(text) > max_length else text

async def perform_request(session, method, url, **kwargs):
    start_time = time.time()
    endpoint = url.replace(BASE_URL, "")
    payload = kwargs.get('json', kwargs.get('params', {}))
    
    try:
        async with session.request(method, url, **kwargs) as resp:
            latency = time.time() - start_time
            status = resp.status
            try:
                response_text = await resp.text()
            except:
                response_text = "N/A"
            
            return {
                "timestamp": datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f'),
                "method": method,
                "endpoint": endpoint,
                "payload": json.dumps(payload),
                "status": status,
                "latency_ms": round(latency * 1000, 2),
                "response": truncate_text(response_text)
            }
    except Exception as e:
        return {
            "timestamp": datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f'),
            "method": method,
            "endpoint": endpoint,
            "payload": json.dumps(payload),
            "status": "ERROR",
            "latency_ms": round((time.time() - start_time) * 1000, 2),
            "response": str(e)
        }

async def post_user(session):
    payload = {
        "name": f"User_{random_string()}",
        "email": f"{random_string()}@example.com",
        "phoneNumber": f"98{random.randint(10000000, 99999999)}",
        "address": f"Address_{random_string()}"
    }
    return await perform_request(session, "POST", f"{BASE_URL}/api/users", json=payload)

async def post_product(session):
    payload = {
        "name": f"Product_{random_string()}",
        "price": round(random.uniform(10.0, 1000.0), 2)
    }
    return await perform_request(session, "POST", f"{BASE_URL}/api/products", json=payload)

async def post_order(session, user_ids, product_ids):
    if not user_ids or not product_ids:
        return {"status": "SKIPPED", "response": "No IDs available"}
    
    payload = {
        "user": {"id": random.choice(user_ids)},
        "product": {"id": random.choice(product_ids)},
        "quantity": random.randint(1, 5)
    }
    return await perform_request(session, "POST", f"{BASE_URL}/api/orders", json=payload)

async def post_log(session):
    payload = {
        "message": f"Log message {random_string()}",
        "timestamp": time.strftime('%Y-%m-%d %H:%M:%S')
    }
    return await perform_request(session, "POST", f"{BASE_URL}/api/logs", json=payload)

async def get_user_search(session):
    return await perform_request(session, "GET", f"{BASE_URL}/api/users/search", params={"name": "User"})

async def get_product_search(session):
    return await perform_request(session, "GET", f"{BASE_URL}/api/products/search", params={"name": "Product"})

async def worker(queue, session, user_ids, product_ids, results):
    while True:
        func = await queue.get()
        try:
            if func == post_order:
                res = await func(session, user_ids, product_ids)
            else:
                res = await func(session)
            results.append(res)
        except Exception as e:
            print(f"Worker error: {e}")
        finally:
            queue.task_done()

async def main():
    results = []
    async with aiohttp.ClientSession() as session:
        print("Fetching valid User and Product IDs...")
        try:
            async with session.get(f"{BASE_URL}/api/users/search", params={"name": ""}) as resp:
                users = await resp.json() if resp.status == 200 else []
            async with session.get(f"{BASE_URL}/api/products/search", params={"name": ""}) as resp:
                products = await resp.json() if resp.status == 200 else []
        except Exception as e:
            print(f"Error fetching initial data: {e}")
            users, products = [], []
        
        user_ids = [u['id'] for u in users if u.get('id') is not None]
        product_ids = [p['id'] for p in products if p.get('id') is not None]

        queue = asyncio.Queue()
        tasks = []
        for _ in range(TOTAL_POSTS):
            tasks.extend([post_user, post_product, post_order, post_log])
        for _ in range(TOTAL_GETS):
            tasks.extend([get_user_search, get_product_search])
            
        random.shuffle(tasks)
        for t in tasks:
            queue.put_nowait(t)

        print(f"Starting load test with {CONCURRENCY} concurrency...")
        start_time = time.time()

        workers = [asyncio.create_task(worker(queue, session, user_ids, product_ids, results)) for _ in range(CONCURRENCY)]
        await queue.join()
        for w in workers: w.cancel()

    end_time = time.time()
    
    # Write to CSV
    csv_file = "load_test_results.csv"
    print(f"Writing results to {csv_file}...")
    with open(csv_file, mode='w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=["timestamp", "method", "endpoint", "payload", "status", "latency_ms", "response"])
        writer.writeheader()
        writer.writerows(results)

    total_requests = len(results)
    print(f"\n--- Load Test Results ---")
    print(f"Total Requests: {total_requests}")
    print(f"Total Time: {end_time - start_time:.2f} seconds")
    print(f"Requests Per Second: {total_requests / (end_time - start_time):.2f}")
    print(f"Results saved to: {csv_file}")

if __name__ == "__main__":
    asyncio.run(main())
