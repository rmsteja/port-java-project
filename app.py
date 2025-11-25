from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)

# NOTE: Use a proper connection pool/manager in production.
def get_db_connection():
    # Adjust to your actual DB; this example uses sqlite3 for illustration.
    conn = sqlite3.connect('app.db')
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/users', methods=['GET'])
def get_users():
    # Safely read optional filter without concatenating into SQL strings.
    name = request.args.get('name')

    conn = get_db_connection()
    cur = conn.cursor()

    try:
        if name:
            # FIX: Use parameterized query to prevent SQL injection.
            cur.execute("SELECT id, name, email FROM users WHERE name = ?", (name,))
        else:
            cur.execute("SELECT id, name, email FROM users")
        rows = cur.fetchall()
        users = [{"id": r["id"], "name": r["name"], "email": r["email"]} for r in rows]
        return jsonify(users), 200
    finally:
        cur.close()
        conn.close()

if __name__ == '__main__':
    # Consider using a WSGI server (gunicorn/uwsgi) in production.
    app.run(host='0.0.0.0', port=5000, debug=False)

