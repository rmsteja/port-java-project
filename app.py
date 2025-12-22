from flask import Flask, request, jsonify
import sqlite3
import os

app = Flask(__name__)

DB_PATH = os.environ.get("DB_PATH", "app.db")


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"])
def get_users():
    """
    Securely fetch users filtered by optional 'name' query parameter.
    Fix: Use parameterized queries instead of string concatenation to prevent SQL injection.
    """
    name = request.args.get("name")

    conn = get_db_connection()
    try:
        cur = conn.cursor()
        if name:
            # Parameterized query prevents SQL injection
            cur.execute("SELECT id, username, email FROM users WHERE username = ?", (name,))
        else:
            cur.execute("SELECT id, username, email FROM users")
        rows = cur.fetchall()
        users = [{"id": row["id"], "username": row["username"], "email": row["email"]} for row in rows]
        return jsonify(users), 200
    finally:
        conn.close()


@app.route("/")
def index():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)), debug=False)

