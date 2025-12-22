from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = 'users.db'


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    # Return rows as dict-like objects
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def users():
    """
    Secure users endpoint preventing SQL injection by using parameterized queries.
    Supports optional query parameter `q` to search by username (case-insensitive, partial match).
    """
    q = request.args.get('q', default=None, type=str)

    with closing(get_db_connection()) as conn, closing(conn.cursor()) as cur:
        if q is None or q.strip() == "":
            # No filter: return all users
            cur.execute("SELECT id, username, email FROM users")
        else:
            # Use parameter binding with LIKE; add wildcards in the parameter, not the SQL string
            # Also normalize to case-insensitive match using LOWER
            search = f"%{q.strip().lower()}%"
            cur.execute(
                """
                SELECT id, username, email
                FROM users
                WHERE LOWER(username) LIKE ?
                """,
                (search,)
            )

        rows = cur.fetchall()
        users_list = [dict(row) for row in rows]
        return jsonify(users_list), 200


@app.route('/')
def index():
    return jsonify({"status": "ok"})


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)

