from flask import Flask, request, jsonify
import sqlite3
import os
import re

app = Flask(__name__)

DB_PATH = os.getenv("DB_PATH", "database.db")


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"]) 
def users():
    """
    Secure implementation of the /users endpoint using parameterized queries.
    Supports optional filtering by 'id' or 'username' via query parameters.
    """
    user_id = request.args.get("id")
    username = request.args.get("username")

    conn = get_db_connection()
    cur = conn.cursor()

    try:
        if user_id is not None:
            # Validate that id is an integer
            try:
                user_id_int = int(user_id)
            except ValueError:
                return jsonify({"error": "id must be an integer"}), 400

            cur.execute("SELECT id, username, email FROM users WHERE id = ?", (user_id_int,))
            rows = cur.fetchall()
        elif username is not None:
            # Optional lightweight input check to avoid extremely long inputs
            if len(username) > 255:
                return jsonify({"error": "username too long"}), 400

            # Parameterized query prevents SQL injection
            cur.execute("SELECT id, username, email FROM users WHERE username = ?", (username,))
            rows = cur.fetchall()
        else:
            # No filters: return all users (safe, no user input in query)
            cur.execute("SELECT id, username, email FROM users")
            rows = cur.fetchall()

        result = [dict(row) for row in rows]
        return jsonify(result)
    finally:
        conn.close()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=False)

