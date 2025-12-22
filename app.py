from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

DB_PATH = 'app.db'


def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    # Return rows as dict-like objects
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def users():
    """
    Secure users endpoint.
    Previously vulnerable to SQL Injection due to string concatenation with user input.
    Now uses parameterized queries to prevent injection.
    """
    name = request.args.get('name')

    with closing(get_db_connection()) as conn:
        cur = conn.cursor()
        if name:
            # Parameterized query prevents SQL injection
            cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
        else:
            # No user-controlled SQL construction
            cur.execute("SELECT id, name, email FROM users")
        rows = cur.fetchall()

    users_list = [{"id": row["id"], "name": row["name"], "email": row["email"]} for row in rows]
    return jsonify(users_list)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

