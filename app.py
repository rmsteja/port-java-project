from flask import Flask, request, jsonify
import sqlite3
from typing import Any, Dict, List, Optional

app = Flask(__name__)

# NOTE: Minimal fix to prevent SQL injection in /users endpoint.
# Use parameterized queries instead of string concatenation.

DB_PATH = "./users.db"


def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route("/users", methods=["GET"])  # Vulnerable endpoint fixed
def list_users():
    """
    Securely list users. Optional filters:
    - name: exact match on user name
    - email: exact match on email
    
    Previously, user input was concatenated into SQL, allowing injection.
    Now we use parameterized queries and a whitelist of filterable columns.
    """
    name: Optional[str] = request.args.get("name")
    email: Optional[str] = request.args.get("email")

    # Build query with bound parameters only
    base_query = "SELECT id, name, email FROM users"
    where_clauses: List[str] = []
    params: List[Any] = []

    if name is not None and name != "":
        where_clauses.append("name = ?")
        params.append(name)
    if email is not None and email != "":
        where_clauses.append("email = ?")
        params.append(email)

    if where_clauses:
        base_query += " WHERE " + " AND ".join(where_clauses)

    # Optional: enforce an upper bound on returned rows to avoid abuse
    base_query += " ORDER BY id ASC LIMIT 500"

    conn = get_db()
    try:
        cur = conn.execute(base_query, params)
        rows = cur.fetchall()
        users = [{"id": r["id"], "name": r["name"], "email": r["email"]} for r in rows]
        return jsonify(users), 200
    finally:
        conn.close()


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"}), 200


if __name__ == "__main__":
    # In production, run behind a WSGI server. Debug disabled by default.
    app.run(host="0.0.0.0", port=8000)
