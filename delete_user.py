import os
import sys
import mysql.connector

db_user = os.environ.get('DB_USERNAME', 'root')
db_password = os.environ.get('DB_PASSWORD')

if not db_password and os.path.exists('.env'):
    with open('.env', 'r') as f:
        for line in f:
            if line.startswith('DB_PASSWORD='):
                db_password = line.strip().split('=', 1)[1].strip('"\'')

target_email = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('TARGET_EMAIL')

if not target_email:
    print("Usage: python delete_user.py <email>")
    sys.exit(1)

if not db_password:
    print("Error: DB_PASSWORD environment variable or .env entry required.")
    sys.exit(1)

conn = mysql.connector.connect(host=os.environ.get('DB_HOST', 'localhost'), user=db_user, password=db_password, database=os.environ.get('DB_NAME', 'travelmate_ai'))
cursor = conn.cursor()
cursor.execute("DELETE FROM users WHERE email=%s", (target_email,))
conn.commit()
print(f"Deleted user with email: {target_email}")
conn.close()
