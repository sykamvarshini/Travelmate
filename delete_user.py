import mysql.connector

try:
    conn = mysql.connector.connect(host='localhost', user='root', password='Travelmate123', database='travelmate_ai')
    cursor = conn.cursor()
    cursor.execute("DELETE FROM users WHERE email='sykamvarshini30@gmail.com'")
    conn.commit()
    cursor.close()
    conn.close()
    print("User deleted successfully")
except Exception as e:
    print("Error:", e)
