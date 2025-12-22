from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = 'users.db'


def get_db_connection():
    # Use isolation_level=None for autocommit behavior only if desired
    # Keep simple, secure connection handling
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def get_users():
    """
    Securely fetch users, optionally filtered by 'name' query parameter.
    Previously vulnerable to SQL injection due to string concatenation.
    """
    name = request.args.get('name')

    try:
        with closing(get_db_connection()) as conn, closing(conn.cursor()) as cur:
            if name:
                # Parameterized query prevents SQL injection
                cur.execute(
                    "SELECT id, name, email FROM users WHERE name = ?",
                    (name,)
                )
            else:
                cur.execute("SELECT id, name, email FROM users")

            rows = cur.fetchall()
            users = [dict(row) for row in rows]
            return jsonify(users), 200
    except sqlite3.Error as e:
        # Avoid leaking internal errors, return generic message
        return jsonify({"error": "Database error"}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

