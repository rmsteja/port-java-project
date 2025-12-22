from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)

# Helper to get DB connection
def get_db():
    conn = sqlite3.connect('app.db')
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/users', methods=['GET'])
def get_users():
    # Securely handle user-supplied input using parameterized queries to prevent SQL injection
    name = request.args.get('name')
    conn = get_db()
    try:
        cur = conn.cursor()
        if name:
            # FIX: use parameter binding instead of string concatenation
            cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
        else:
            cur.execute("SELECT id, name, email FROM users")
        rows = cur.fetchall()
        users = [dict(row) for row in rows]
        return jsonify(users), 200
    finally:
        conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)

