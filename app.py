from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = "./users.db"


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    # Return rows as dictionaries for safe JSON serialization
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"])  # Fixed: use parameterized queries to prevent SQL injection
def users():
    name = request.args.get("name")
    try:
        with closing(get_db_connection()) as conn, closing(conn.cursor()) as cur:
            if name:
                # Use parameterized query instead of string concatenation
                cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
            else:
                cur.execute("SELECT id, name, email FROM users")
            rows = cur.fetchall()
            result = [dict(row) for row in rows]
            return jsonify(result), 200
    except sqlite3.Error as e:
        return jsonify({"error": "database_error", "message": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)

