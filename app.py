from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)


def get_db_connection():
    conn = sqlite3.connect('app.db')
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def users():
    # Safely handle optional query filter by name
    name = request.args.get('name')

    conn = get_db_connection()
    cur = conn.cursor()

    try:
        if name:
            # Use parameterized query to prevent SQL injection
            cur.execute('SELECT id, name, email FROM users WHERE name = ?', (name,))
        else:
            cur.execute('SELECT id, name, email FROM users')

        rows = cur.fetchall()
        result = [dict(row) for row in rows]
        return jsonify(result), 200
    finally:
        conn.close()


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
