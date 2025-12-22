from flask import Flask, request, jsonify
import sqlite3
from contextlib import closing

app = Flask(__name__)

# Helper to get DB connection; adjust path/DSN as needed for your environment
DB_PATH = 'app.db'

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

@app.route('/users', methods=['GET'])
def get_users():
    # Expect a "name" query parameter. Return 400 if missing.
    name = request.args.get('name')
    if name is None:
        return jsonify({'error': 'Missing required query parameter: name'}), 400

    # Use parameterized queries to prevent SQL Injection.
    # Never concatenate user input into SQL strings.
    try:
        with closing(get_db_connection()) as conn:
            with closing(conn.cursor()) as cur:
                # Parameterized query: the driver safely handles escaping
                cur.execute(
                    'SELECT id, name, email FROM users WHERE name = ?',
                    (name,)
                )
                rows = cur.fetchall()
                users = [dict(row) for row in rows]
                return jsonify({'users': users}), 200
    except Exception as e:
        # Avoid leaking internals; log in real apps. Return a generic error.
        return jsonify({'error': 'Failed to fetch users'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

