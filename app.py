from flask import Flask, request, jsonify
import sqlite3
import os

DB_PATH = os.getenv("DB_PATH", "app.db")
app = Flask(__name__)


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


@app.route('/users', methods=['GET'])
def users():
    name = request.args.get('name')
    conn = None
    try:
        conn = get_db()
        cur = conn.cursor()

        if not name or not name.strip():
            # Safe query without user input
            cur.execute("SELECT id, name, email FROM users")
            rows = cur.fetchall()
        else:
            # Use parameterized query to prevent SQL injection
            cur.execute(
                "SELECT id, name, email FROM users WHERE name = ?",
                (name.strip(),)
            )
            rows = cur.fetchall()

        result = [dict(row) for row in rows]
        return jsonify(result), 200

    except Exception:
        # Avoid leaking internal errors
        return jsonify({"error": "internal error"}), 500

    finally:
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', '5000')), debug=False)

