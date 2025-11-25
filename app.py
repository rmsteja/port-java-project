from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)


def get_db():
    conn = sqlite3.connect('app.db', check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def get_users():
    # Use parameterized queries to prevent SQL injection
    name = request.args.get('name')

    conn = get_db()
    cur = conn.cursor()

    if name:
        # Safe, parameterized query instead of string concatenation
        cur.execute(
            "SELECT id, name, email FROM users WHERE name = ?",
            (name,)
        )
    else:
        cur.execute("SELECT id, name, email FROM users")

    rows = cur.fetchall()
    users = [dict(row) for row in rows]

    return jsonify(users), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

