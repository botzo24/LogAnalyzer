import random

FILENAME = "large_test_logs.txt"
NUM_LOGS = 10000

LEVELS = ["INFO", "WARN", "ERROR", "DEBUG"]
SERVICES = [
    "AuthService", "PaymentGateway", "OrderService",
    "InventoryManager", "NotificationService", "UserMicroservice"
]
MESSAGES = [
    "Database connection established",
    "User login failed due to invalid credentials",
    "Payment processed successfully for transaction",
    "Timeout waiting for upstream response",
    "Cache miss for key user_session",
    "Disk usage exceeding 85 percent",
    "NullPointerException in handler method",
    "New order created with ID",
    "Email sent to user"
]

def generate_log_line():
    level = random.choice(LEVELS)
    service = random.choice(SERVICES)
    base_msg = random.choice(MESSAGES)
    unique_id = random.randint(1000, 99999)

    return f"{level} {service} {base_msg} #{unique_id}\n"

with open(FILENAME, "w") as f:
    for _ in range(NUM_LOGS):
        f.write(generate_log_line())

print(f"Successfully generated {FILENAME} with {NUM_LOGS} lines.")