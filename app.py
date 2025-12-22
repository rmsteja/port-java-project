from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = 'users.db'


def get_db_connection():
    # Use isolation_level=None to enable autocommit for simple read-only queries
    conn = sqlite3.connect(DB_PATH, isolation_level=None)
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def get_users():
    # Safely handle user-supplied input using parameterized queries
    name = request.args.get('name')

    try:
        with closing(get_db_connection()) as conn:
            cur = conn.cursor()
            if name:
                # Use parameterized query to prevent SQL injection
                cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
            else:
                cur.execute("SELECT id, name, email FROM users")
            rows = cur.fetchall()

            users = [dict(row) for row in rows]
            return jsonify(users), 200
    except sqlite3.Error as e:
        # Avoid leaking DB details; return generic error
        return jsonify({"error": "Database error"}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
