from flask import Flask, request, jsonify
import sqlite3
import os

app = Flask(__name__)

DB_PATH = os.getenv("DB_PATH", "app.db")


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"]) 
def users():
    """
    Secure users endpoint.
    Previously vulnerable due to direct string concatenation of user input
    in SQL query. This version uses parameterized queries to prevent SQL injection.
    Behavior:
    - If a "name" query parameter is provided, filter users by exact username match.
    - If a "search" query parameter is provided, perform a LIKE search.
    - If neither is provided, return all users.
    """
    name = request.args.get("name")
    search = request.args.get("search")

    conn = get_db_connection()
    try:
        if name:
            # Parameterized exact-match query
            cur = conn.execute(
                "SELECT id, username, email FROM users WHERE username = ?",
                (name,)
            )
        elif search:
            # Parameterized LIKE query (still safe, the wildcard is part of the parameter)
            like = f"%{search}%"
            cur = conn.execute(
                "SELECT id, username, email FROM users WHERE username LIKE ?",
                (like,)
            )
        else:
            # Return all users
            cur = conn.execute("SELECT id, username, email FROM users")

        rows = cur.fetchall()
        users_list = [dict(row) for row in rows]
        return jsonify({"users": users_list}), 200
    finally:
        conn.close()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=os.getenv("FLASK_DEBUG", "false").lower() == "true")

