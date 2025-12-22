from flask import Flask, request, jsonify
import os
import sqlite3

app = Flask(__name__)

# Use a safe, parameterized approach for all SQL statements to prevent injection.
def get_db_connection():
    db_path = os.getenv("DB_PATH", "app.db")
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn

@app.route("/users", methods=["GET"]) 
def get_users():
    """
    Securely fetch users. If a "name" query parameter is provided, filter by name using
    parameterized SQL instead of string concatenation.
    Example: /users?name=alice
    """
    name = request.args.get("name")
    conn = get_db_connection()
    cur = conn.cursor()

    # Before: vulnerable pattern (example) — DO NOT USE
    # query = f"SELECT id, name, email FROM users WHERE name = '{name}'"  # SQL injection risk
    # cur.execute(query)

    if name:
        # Safe parameterized query
        cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
    else:
        cur.execute("SELECT id, name, email FROM users")

    rows = cur.fetchall()
    conn.close()

    return jsonify([dict(row) for row in rows])

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=False)

