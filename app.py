from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = "app.db"


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"])
def get_users():
    # Secure implementation: use parameterized queries instead of string concatenation
    username = request.args.get("username")
    with closing(get_db_connection()) as conn, closing(conn.cursor()) as cur:
        if username:
            # Parameterized query prevents SQL injection
            cur.execute(
                "SELECT id, username, email FROM users WHERE username = ?",
                (username,),
            )
        else:
            cur.execute("SELECT id, username, email FROM users")
        rows = cur.fetchall()
        users = [dict(row) for row in rows]
        return jsonify(users), 200


@app.route("/health", methods=["GET"])  # keep a simple health check
def health():
    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

