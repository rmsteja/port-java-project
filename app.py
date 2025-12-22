from flask import Flask, request, jsonify
import os
import sqlite3

app = Flask(__name__)

DB_PATH = os.getenv("DB_PATH", "./app.db")


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"])
def get_users():
    """
    Secure implementation of /users endpoint using parameterized queries to prevent SQL injection.
    Supports optional filters: id, username, email. Returns JSON list of users.
    """
    # Collect optional filters from query string
    user_id = request.args.get("id")
    username = request.args.get("username")
    email = request.args.get("email")

    # Build query safely with placeholders
    base_sql = "SELECT id, username, email FROM users"
    where_clauses = []
    params = []

    if user_id is not None and user_id != "":
        # Ensure id is an integer to avoid type confusion
        try:
            user_id_int = int(user_id)
            where_clauses.append("id = ?")
            params.append(user_id_int)
        except ValueError:
            return jsonify({"error": "id must be an integer"}), 400

    if username:
        where_clauses.append("username = ?")
        params.append(username)

    if email:
        where_clauses.append("email = ?")
        params.append(email)

    if where_clauses:
        sql = f"{base_sql} WHERE " + " AND ".join(where_clauses)
    else:
        sql = base_sql

    conn = get_db_connection()
    try:
        cur = conn.cursor()
        cur.execute(sql, params)
        rows = cur.fetchall()
        users = [{"id": row["id"], "username": row["username"], "email": row["email"]} for row in rows]
        return jsonify(users), 200
    finally:
        conn.close()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("PORT", "5000")), debug=False)

