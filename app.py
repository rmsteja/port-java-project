from flask import Flask, request, jsonify
import sqlite3

app = Flask(__name__)

def get_db_connection():
    conn = sqlite3.connect('database.db')
    conn.row_factory = sqlite3.Row
    return conn

# Secure implementation of /users endpoint using parameterized queries to prevent SQL injection
@app.route('/users', methods=['GET'])
def get_users():
    # Accept optional filters while avoiding direct string concatenation
    name = request.args.get('name')
    email = request.args.get('email')

    query = "SELECT id, name, email FROM users"
    params = []
    clauses = []

    if name is not None and name != "":
        clauses.append("name LIKE ?")
        params.append(f"%{name}%")

    if email is not None and email != "":
        clauses.append("email = ?")
        params.append(email)

    if clauses:
        query += " WHERE " + " AND ".join(clauses)

    query += " ORDER BY id ASC"

    conn = get_db_connection()
    try:
        cur = conn.cursor()
        # Use parameterized query to prevent SQL injection
        cur.execute(query, params)
        rows = cur.fetchall()
        users = [{"id": row["id"], "name": row["name"], "email": row["email"]} for row in rows]
        return jsonify(users), 200
    finally:
        conn.close()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

