import asyncio
import aiohttp
import time
import random
import string

# BASE_URL = "https://esewaprod.sudarshan-uprety.com.np"
BASE_URL = "http://localhost:8080" # Uncomment for local testing

CONCURRENCY = 1000  # Number of simultaneous requests
TOTAL_POSTS = 10000 # Total POST requests per endpoint
TOTAL_GETS = 5000   # Total GET requests per endpoint

def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))

async def post_user(session):
    payload = {
        "name": f"User_{random_string()}",
        "email": f"{random_string()}@example.com",
        "phoneNumber": f"98{random.randint(10000000, 99999999)}",
        "address": f"Address_{random_string()}"
    }
    async with session.post(f"{BASE_URL}/api/users", json=payload) as resp:
        return resp.status

async def post_product(session):
    payload = {
        "name": f"Product_{random_string()}",
        "price": round(random.uniform(10.0, 1000.0), 2)
    }
    async with session.post(f"{BASE_URL}/api/products", json=payload) as resp:
        return resp.status

async def post_order(session, user_ids, product_ids):
    if not user_ids or not product_ids:
        return 400
    
    payload = {
        "user": {"id": random.choice(user_ids)},
        "product": {"id": random.choice(product_ids)},
        "quantity": random.randint(1, 5)
    }
    async with session.post(f"{BASE_URL}/api/orders", json=payload) as resp:
        return resp.status

async def post_log(session):
    payload = {
        "message": f"Log message {random_string()}",
        "timestamp": time.strftime('%Y-%m-%d %H:%M:%S')
    }
    async with session.post(f"{BASE_URL}/api/logs", json=payload) as resp:
        return resp.status

async def get_user_search(session):
    async with session.get(f"{BASE_URL}/api/users/search", params={"name": "User"}) as resp:
        return resp.status

async def get_product_search(session):
    async with session.get(f"{BASE_URL}/api/products/search", params={"name": "Product"}) as resp:
        return resp.status

async def worker(queue, session, user_ids, product_ids):
    while True:
        func = await queue.get()
        try:
            if func == post_order:
                status = await func(session, user_ids, product_ids)
            else:
                status = await func(session)
        except Exception as e:
            print(f"Error in {func.__name__}: {e}")
        finally:
            queue.task_done()

async def main():
    async with aiohttp.ClientSession() as session:
        # Fetch valid IDs first
        print("Fetching valid User and Product IDs...")
        try:
            async with session.get(f"{BASE_URL}/api/users/search", params={"name": ""}) as resp:
                if resp.status == 200:
                    users = await resp.json()
                else:
                    print(f"Failed to fetch users: {resp.status}")
                    users = []
            
            async with session.get(f"{BASE_URL}/api/products/search", params={"name": ""}) as resp:
                if resp.status == 200:
                    products = await resp.json()
                else:
                    print(f"Failed to fetch products: {resp.status}")
                    products = []
        except Exception as e:
            print(f"Error fetching initial data: {e}")
            users = []
            products = []
        
        user_ids = [u['id'] for u in users if u.get('id') is not None]
        product_ids = [p['id'] for p in products if p.get('id') is not None]
        
        if not user_ids or not product_ids:
            print("Warning: No valid users or products found. Order creation will be skipped.")

        queue = asyncio.Queue()
        
        # Fill queue with tasks
        tasks = []
        for _ in range(TOTAL_POSTS):
            tasks.append(post_user)
            tasks.append(post_product)
            tasks.append(post_order)
            tasks.append(post_log)
        
        for _ in range(TOTAL_GETS):
            tasks.append(get_user_search)
            tasks.append(get_product_search)
            
        random.shuffle(tasks)
        for t in tasks:
            queue.put_nowait(t)

        print(f"Starting load test with {CONCURRENCY} concurrency...")
        start_time = time.time()

        workers = [asyncio.create_task(worker(queue, session, user_ids, product_ids)) for _ in range(CONCURRENCY)]
        
        await queue.join()
        
        for w in workers:
            w.cancel()

    end_time = time.time()
    total_requests = (TOTAL_POSTS * 4) + (TOTAL_GETS * 2)
    print(f"\n--- Load Test Results ---")
    print(f"Total Requests: {total_requests}")
    print(f"Total Time: {end_time - start_time:.2f} seconds")
    print(f"Requests Per Second: {total_requests / (end_time - start_time):.2f}")

if __name__ == "__main__":
    asyncio.run(main())
