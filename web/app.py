from flask import Flask, render_template, request, redirect, url_for, session, jsonify
import sqlite3
import bcrypt
from functools import wraps
import os
import subprocess
import socket
import struct
import json
from datetime import datetime, timedelta
import re
import shutil
import time
import hmac
import hashlib
import urllib.request
import urllib.error
import urllib.parse

app = Flask(__name__)

CONFIG_PATH = '../config/panel_config.json'

def load_panel_config():
    if not os.path.exists(CONFIG_PATH):
        return {}
    try:
        with open(CONFIG_PATH, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception:
        return {}

panel_config = load_panel_config()

web_config = panel_config.get('web_server', {})
minecraft_config = panel_config.get('minecraft_server', {})
db_config = panel_config.get('database', {})

app.secret_key = web_config.get('secret_key', os.urandom(24))
app.config['SESSION_COOKIE_NAME'] = panel_config.get('authentication', {}).get('session_cookie_name', 'mmorpg_session')
app.config['SESSION_COOKIE_HTTPONLY'] = panel_config.get('authentication', {}).get('session_cookie_httponly', True)
app.config['SESSION_COOKIE_SECURE'] = panel_config.get('authentication', {}).get('session_cookie_secure', False)
app.config['PERMANENT_SESSION_LIFETIME'] = timedelta(
    seconds=panel_config.get('web_server', {}).get('session_timeout_seconds', 3600)
)

DB_PATH = db_config.get('universal_db_path', '../config/data/universal.db')
SERVER_DIR = minecraft_config.get('server_directory', '../minecraft-server')
RCON_HOST = minecraft_config.get('rcon_host', 'localhost')
RCON_PORT = minecraft_config.get('rcon_port', 25575)
RCON_PASSWORD = minecraft_config.get('rcon_password', 'minecraft')
BACKUP_DIR = panel_config.get('backup', {}).get('backup_directory', '../backups')

RATE_LIMITS = panel_config.get('rate_limiting', {
    'enabled': True,
    'console_commands_per_minute': 10,
    'api_requests_per_minute': 60,
    'login_attempts_per_hour': 10
})
RATE_STATE = {
    'api': {},
    'console': {},
    'login': {}
}
LOCKOUTS = {}
LOCKOUT_MINUTES = panel_config.get('authentication', {}).get('lockout_duration_minutes', 15)

INTEGRATIONS_CONFIG = panel_config.get('integrations', {})
WEBHOOK_SECRET = INTEGRATIONS_CONFIG.get('webhook_secret', '')
WEBHOOK_MAX_ATTEMPTS = INTEGRATIONS_CONFIG.get('webhook_max_attempts', 3)
WEBHOOK_RETRY_SECONDS = INTEGRATIONS_CONFIG.get('webhook_retry_seconds', 2)

CACHE_TTL_SECONDS = 30
CACHE_STORE = {}

def get_cache(key):
    entry = CACHE_STORE.get(key)
    if not entry:
        return None
    value, expires_at = entry
    if datetime.now() > expires_at:
        del CACHE_STORE[key]
        return None
    return value

def set_cache(key, value, ttl_seconds=CACHE_TTL_SECONDS):
    CACHE_STORE[key] = (value, datetime.now() + timedelta(seconds=ttl_seconds))

def get_client_ip():
    return request.headers.get('X-Forwarded-For', request.remote_addr)

def rate_limit(bucket, key, limit, window_seconds):
    now = time.time()
    state = RATE_STATE[bucket].setdefault(key, [])
    state[:] = [t for t in state if now - t < window_seconds]
    if len(state) >= limit:
        return False
    state.append(now)
    return True

def is_locked_out(key):
    until = LOCKOUTS.get(key)
    return until is not None and until > datetime.now()

def lock_out(key):
    LOCKOUTS[key] = datetime.now() + timedelta(minutes=LOCKOUT_MINUTES)

UUID_RE = re.compile(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$')

def is_valid_uuid(value):
    return bool(value and UUID_RE.match(value))

def log_admin_action(action, payload=None):
    try:
        conn = get_db_connection()
        c = conn.cursor()
        c.execute("""
            CREATE TABLE IF NOT EXISTS system_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT,
                category TEXT,
                message TEXT,
                timestamp INTEGER
            )
        """)
        message = json.dumps({
            'action': action,
            'user_id': session.get('user_id'),
            'ip': get_client_ip(),
            'payload': payload or {}
        })
        c.execute(
            "INSERT INTO system_logs (level, category, message, timestamp) VALUES (?, ?, ?, ?)",
            ('INFO', 'admin', message, int(datetime.now().timestamp()))
        )
        conn.commit()
        conn.close()
    except Exception:
        pass

def get_webhook_secret():
    if WEBHOOK_SECRET:
        return str(WEBHOOK_SECRET)
    if isinstance(app.secret_key, bytes):
        return app.secret_key.hex()
    return str(app.secret_key)

def record_webhook_delivery(webhook_id, event, payload, status_code, success, error, attempts):
    try:
        conn = get_db_connection()
        c = conn.cursor()
        c.execute(
            "INSERT INTO webhook_deliveries (webhook_id, event, payload, status_code, success, error, attempts, delivered_at) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                webhook_id,
                event,
                payload,
                status_code,
                1 if success else 0,
                error,
                attempts,
                int(datetime.now().timestamp())
            )
        )
        conn.commit()
        conn.close()
    except Exception:
        pass

def send_webhook_with_retry(webhook, event, payload):
    payload_json = json.dumps({'event': event, 'data': payload})
    timestamp = str(int(time.time()))
    signature = hmac.new(
        get_webhook_secret().encode('utf-8'),
        f"{timestamp}.{payload_json}".encode('utf-8'),
        hashlib.sha256
    ).hexdigest()

    headers = {
        'Content-Type': 'application/json',
        'X-Webhook-Event': event,
        'X-Webhook-Timestamp': timestamp,
        'X-Webhook-Signature': signature
    }

    last_status = None
    last_error = None
    for attempt in range(1, WEBHOOK_MAX_ATTEMPTS + 1):
        try:
            req = urllib.request.Request(
                webhook['url'],
                data=payload_json.encode('utf-8'),
                headers=headers,
                method='POST'
            )
            with urllib.request.urlopen(req, timeout=5) as resp:
                last_status = resp.getcode()
            success = 200 <= last_status < 300
            record_webhook_delivery(
                webhook['id'], event, payload_json, last_status, success, None, attempt
            )
            if success:
                return True
            last_error = f"HTTP {last_status}"
        except Exception as e:
            last_error = str(e)
            record_webhook_delivery(
                webhook['id'], event, payload_json, last_status, False, last_error, attempt
            )
        if attempt < WEBHOOK_MAX_ATTEMPTS:
            time.sleep(WEBHOOK_RETRY_SECONDS * attempt)
    return False

def dispatch_webhooks(event, payload):
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, url, events, is_active FROM integrations_webhooks WHERE is_active = 1")
    rows = c.fetchall()
    conn.close()

    sent = 0
    for r in rows:
        events_value = (r['events'] or '').strip()
        if events_value:
            allowed = {e.strip() for e in events_value.split(',') if e.strip()}
            if event not in allowed:
                continue
        webhook = {'id': r['id'], 'url': r['url']}
        if send_webhook_with_retry(webhook, event, payload):
            sent += 1
    return sent

def get_db_connection():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_postlaunch_tables():
    conn = get_db_connection()
    c = conn.cursor()

    c.execute("""
        CREATE TABLE IF NOT EXISTS guilds (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            tag TEXT UNIQUE,
            leader_uuid TEXT NOT NULL,
            description TEXT,
            created_at INTEGER,
            max_members INTEGER DEFAULT 20,
            bank_balance REAL DEFAULT 0.0
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS guild_members (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            guild_id INTEGER NOT NULL,
            player_uuid TEXT NOT NULL,
            role TEXT DEFAULT 'member',
            joined_at INTEGER,
            contributed REAL DEFAULT 0.0,
            UNIQUE(guild_id, player_uuid)
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS friends (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            friend_uuid TEXT NOT NULL,
            status TEXT DEFAULT 'pending',
            created_at INTEGER,
            UNIQUE(player_uuid, friend_uuid)
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS mail_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sender_uuid TEXT,
            receiver_uuid TEXT NOT NULL,
            subject TEXT,
            content TEXT NOT NULL,
            sent_at INTEGER,
            is_read INTEGER DEFAULT 0
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS private_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sender_uuid TEXT NOT NULL,
            receiver_uuid TEXT NOT NULL,
            content TEXT NOT NULL,
            sent_at INTEGER,
            is_read INTEGER DEFAULT 0
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS professions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            description TEXT,
            max_level INTEGER DEFAULT 100
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS player_professions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            profession_id INTEGER NOT NULL,
            level INTEGER DEFAULT 1,
            experience INTEGER DEFAULT 0,
            last_updated INTEGER,
            UNIQUE(player_uuid, profession_id)
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS pvp_arenas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            world TEXT NOT NULL,
            x REAL,
            y REAL,
            z REAL,
            radius REAL DEFAULT 25.0,
            is_active INTEGER DEFAULT 1
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS pvp_rankings (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            rating INTEGER DEFAULT 1000,
            wins INTEGER DEFAULT 0,
            losses INTEGER DEFAULT 0,
            last_updated INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS pvp_tournaments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            status TEXT DEFAULT 'scheduled',
            scheduled_at INTEGER,
            created_at INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            type TEXT,
            config_json TEXT,
            status TEXT DEFAULT 'scheduled',
            created_at INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS integrations_webhooks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            url TEXT NOT NULL,
            events TEXT,
            is_active INTEGER DEFAULT 1,
            created_at INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS webhook_deliveries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            webhook_id INTEGER NOT NULL,
            event TEXT NOT NULL,
            payload TEXT NOT NULL,
            status_code INTEGER,
            success INTEGER DEFAULT 0,
            error TEXT,
            attempts INTEGER DEFAULT 0,
            delivered_at INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS metrics (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            key TEXT NOT NULL,
            value REAL,
            timestamp INTEGER
        )
    """)

    c.execute("""
        CREATE TABLE IF NOT EXISTS backups (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            path TEXT NOT NULL,
            size INTEGER DEFAULT 0,
            created_at INTEGER,
            status TEXT DEFAULT 'created'
        )
    """)

    c.execute("CREATE INDEX IF NOT EXISTS idx_guilds_leader ON guilds(leader_uuid)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_guild_members_guild ON guild_members(guild_id)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_friends_player ON friends(player_uuid)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_pvp_rankings_rating ON pvp_rankings(rating)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_events_status ON events(status)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_metrics_key ON metrics(key)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_backups_created ON backups(created_at)")
    c.execute("CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_event ON webhook_deliveries(event)")

    conn.commit()
    conn.close()

def ensure_default_professions():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT COUNT(*) AS count FROM professions")
    count = c.fetchone()[0]
    if count == 0:
        defaults = [
            ("Minería avanzada", "Extracción y refinado de minerales", 100),
            ("Herrería", "Creación de armas y armaduras", 100),
            ("Alquimia", "Pociones y consumibles", 100),
            ("Encantamiento avanzado", "Mejora de equipo", 100),
            ("Cocina", "Comida y buffs", 100)
        ]
        c.executemany("INSERT INTO professions (name, description, max_level) VALUES (?, ?, ?)", defaults)
        conn.commit()
    conn.close()

init_postlaunch_tables()
ensure_default_professions()

def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'user_id' not in session:
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

@app.before_request
def enforce_rate_limits():
    if not RATE_LIMITS.get('enabled', True):
        return None
    ip = get_client_ip()
    if request.endpoint == 'login' and request.method == 'POST':
        if is_locked_out(ip):
            return render_template('login.html', error='Demasiados intentos. Intenta más tarde.'), 429
        limit = RATE_LIMITS.get('login_attempts_per_hour', 10)
        if not rate_limit('login', ip, limit, 3600):
            lock_out(ip)
            return render_template('login.html', error='Demasiados intentos. Intenta más tarde.'), 429
    elif request.path.startswith('/api/console'):
        limit = RATE_LIMITS.get('console_commands_per_minute', 10)
        if not rate_limit('console', ip, limit, 60):
            return jsonify({'error': 'Rate limit exceeded'}), 429
    elif request.path.startswith('/api/'):
        limit = RATE_LIMITS.get('api_requests_per_minute', 60)
        if not rate_limit('api', ip, limit, 60):
            return jsonify({'error': 'Rate limit exceeded'}), 429
    return None

@app.route('/')
@login_required
def index():
    return render_template('dashboard.html')

@app.route('/social')
@login_required
def social():
    return render_template('social.html')

@app.route('/professions')
@login_required
def professions():
    return render_template('professions.html')

@app.route('/pvp')
@login_required
def pvp():
    return render_template('pvp.html')

@app.route('/events')
@login_required
def events():
    return render_template('events.html')

@app.route('/integrations')
@login_required
def integrations():
    return render_template('integrations.html')

@app.route('/optimization')
@login_required
def optimization():
    return render_template('optimization.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']
        
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("SELECT id, password_hash FROM admin_users WHERE username = ?", (username,))
        user = c.fetchone()
        conn.close()
        
        if user and bcrypt.checkpw(password.encode(), user[1].encode()):
            session['user_id'] = user[0]
            session.permanent = True
            ip = get_client_ip()
            LOCKOUTS.pop(ip, None)
            RATE_STATE.get('login', {}).pop(ip, None)
            return redirect(url_for('index'))
        return render_template('login.html', error='Invalid credentials')
    
    return render_template('login.html')

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('login'))

@app.route('/api/social/guilds')
@login_required
def api_guilds():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, tag, leader_uuid, description FROM guilds ORDER BY name")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'tag': r['tag'],
        'leader_uuid': r['leader_uuid'],
        'description': r['description']
    } for r in rows])

@app.route('/api/social/guilds', methods=['POST'])
@login_required
def api_create_guild():
    data = request.json or {}
    name = data.get('name')
    tag = data.get('tag')
    leader_uuid = data.get('leader_uuid')
    description = data.get('description', '')
    if not name or not is_valid_uuid(leader_uuid):
        return jsonify({'error': 'Missing name or leader_uuid'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO guilds (name, tag, leader_uuid, description, created_at) VALUES (?, ?, ?, ?, ?)",
        (name, tag, leader_uuid, description, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('create_guild', {'name': name, 'leader_uuid': leader_uuid})
    return jsonify({'success': True})

@app.route('/api/social/friends/request', methods=['POST'])
@login_required
def api_friend_request():
    data = request.json or {}
    from_uuid = data.get('from_uuid')
    to_uuid = data.get('to_uuid')
    if not is_valid_uuid(from_uuid) or not is_valid_uuid(to_uuid):
        return jsonify({'error': 'Missing from_uuid or to_uuid'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT OR IGNORE INTO friends (player_uuid, friend_uuid, status, created_at) VALUES (?, ?, 'pending', ?)",
        (from_uuid, to_uuid, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/social/friends/accept', methods=['POST'])
@login_required
def api_friend_accept():
    data = request.json or {}
    from_uuid = data.get('from_uuid')
    to_uuid = data.get('to_uuid')
    if not is_valid_uuid(from_uuid) or not is_valid_uuid(to_uuid):
        return jsonify({'error': 'Missing from_uuid or to_uuid'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "UPDATE friends SET status = 'accepted' WHERE player_uuid = ? AND friend_uuid = ?",
        (from_uuid, to_uuid)
    )
    c.execute(
        "INSERT OR IGNORE INTO friends (player_uuid, friend_uuid, status, created_at) VALUES (?, ?, 'accepted', ?)",
        (to_uuid, from_uuid, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    return jsonify({'success': True})

@app.route('/api/social/mail/send', methods=['POST'])
@login_required
def api_mail_send():
    data = request.json or {}
    to_uuid = data.get('to_uuid')
    subject = data.get('subject', '')
    content = data.get('content', '')
    if not is_valid_uuid(to_uuid) or not content:
        return jsonify({'error': 'Missing to_uuid or content'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO mail_messages (sender_uuid, receiver_uuid, subject, content, sent_at) VALUES (?, ?, ?, ?, ?)",
        (None, to_uuid, subject, content, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('mail_send', {'to_uuid': to_uuid})
    return jsonify({'success': True})

@app.route('/api/social/messages/send', methods=['POST'])
@login_required
def api_social_message_send():
    data = request.json or {}
    sender_uuid = data.get('sender_uuid')
    receiver_uuid = data.get('receiver_uuid')
    content = data.get('content', '')
    if not is_valid_uuid(sender_uuid) or not is_valid_uuid(receiver_uuid) or not content:
        return jsonify({'error': 'Missing sender_uuid, receiver_uuid or content'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO private_messages (sender_uuid, receiver_uuid, content, sent_at) VALUES (?, ?, ?, ?)",
        (sender_uuid, receiver_uuid, content, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('social_message_send', {'sender_uuid': sender_uuid, 'receiver_uuid': receiver_uuid})
    return jsonify({'success': True})

@app.route('/api/social/messages/<uuid>')
@login_required
def api_social_messages(uuid):
    if not is_valid_uuid(uuid):
        return jsonify({'error': 'UUID inválido'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "SELECT sender_uuid, receiver_uuid, content, sent_at, is_read FROM private_messages "
        "WHERE sender_uuid = ? OR receiver_uuid = ? ORDER BY sent_at DESC LIMIT 100",
        (uuid, uuid)
    )
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'sender_uuid': r['sender_uuid'],
        'receiver_uuid': r['receiver_uuid'],
        'content': r['content'],
        'sent_at': r['sent_at'],
        'is_read': r['is_read']
    } for r in rows])

@app.route('/api/professions')
@login_required
def api_professions():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, max_level FROM professions ORDER BY name")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'max_level': r['max_level']
    } for r in rows])

@app.route('/api/professions/update', methods=['POST'])
@login_required
def api_professions_update():
    data = request.json or {}
    uuid = data.get('uuid')
    profession_id = data.get('profession_id')
    level = data.get('level', 1)
    experience = data.get('experience', 0)
    if not is_valid_uuid(uuid) or not profession_id:
        return jsonify({'error': 'Missing uuid or profession_id'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO player_professions (player_uuid, profession_id, level, experience, last_updated) "
        "VALUES (?, ?, ?, ?, ?) "
        "ON CONFLICT(player_uuid, profession_id) DO UPDATE SET level = excluded.level, experience = excluded.experience, last_updated = excluded.last_updated",
        (uuid, profession_id, level, experience, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('update_profession', {'uuid': uuid, 'profession_id': profession_id})
    return jsonify({'success': True})

@app.route('/api/pvp/arenas')
@login_required
def api_pvp_arenas():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, world, x, y, z, radius FROM pvp_arenas ORDER BY name")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'world': r['world'],
        'x': r['x'],
        'y': r['y'],
        'z': r['z'],
        'radius': r['radius']
    } for r in rows])

@app.route('/api/pvp/arenas/create', methods=['POST'])
@login_required
def api_pvp_arenas_create():
    data = request.json or {}
    name = data.get('name')
    world = data.get('world')
    x = data.get('x')
    y = data.get('y')
    z = data.get('z')
    radius = data.get('radius', 25)
    if not name or not world:
        return jsonify({'error': 'Missing name or world'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO pvp_arenas (name, world, x, y, z, radius) VALUES (?, ?, ?, ?, ?, ?)",
        (name, world, x, y, z, radius)
    )
    conn.commit()
    conn.close()
    log_admin_action('create_pvp_arena', {'name': name, 'world': world})
    return jsonify({'success': True})

@app.route('/api/pvp/rankings')
@login_required
def api_pvp_rankings():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT player_uuid, rating, wins, losses FROM pvp_rankings ORDER BY rating DESC LIMIT 50")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'player_uuid': r['player_uuid'],
        'rating': r['rating'],
        'wins': r['wins'],
        'losses': r['losses']
    } for r in rows])

@app.route('/api/pvp/tournaments')
@login_required
def api_pvp_tournaments():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, status, scheduled_at FROM pvp_tournaments ORDER BY id DESC")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'status': r['status'],
        'scheduled_at': r['scheduled_at']
    } for r in rows])

@app.route('/api/pvp/tournaments/create', methods=['POST'])
@login_required
def api_pvp_tournaments_create():
    data = request.json or {}
    name = data.get('name')
    scheduled_at = data.get('scheduled_at')
    if not name:
        return jsonify({'error': 'Missing name'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO pvp_tournaments (name, status, scheduled_at, created_at) VALUES (?, 'scheduled', ?, ?)",
        (name, scheduled_at, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('create_pvp_tournament', {'name': name, 'scheduled_at': scheduled_at})
    return jsonify({'success': True})

@app.route('/api/events')
@login_required
def api_events():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, type, status FROM events ORDER BY id DESC")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'type': r['type'],
        'status': r['status']
    } for r in rows])

@app.route('/api/events/create', methods=['POST'])
@login_required
def api_events_create():
    data = request.json or {}
    name = data.get('name')
    event_type = data.get('type')
    config = data.get('config', '{}')
    if isinstance(config, dict):
        config = json.dumps(config)
    if not name or not event_type:
        return jsonify({'error': 'Missing name or type'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO events (name, type, config_json, status, created_at) VALUES (?, ?, ?, 'scheduled', ?)",
        (name, event_type, config, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('create_event', {'name': name, 'type': event_type})
    return jsonify({'success': True})

@app.route('/api/integrations/webhooks')
@login_required
def api_webhooks():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT id, name, url, events FROM integrations_webhooks ORDER BY id DESC")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'id': r['id'],
        'name': r['name'],
        'url': r['url'],
        'events': r['events']
    } for r in rows])

@app.route('/api/integrations/webhooks/create', methods=['POST'])
@login_required
def api_webhooks_create():
    data = request.json or {}
    name = data.get('name')
    url_value = data.get('url')
    events_value = data.get('events', '')
    if not name or not url_value:
        return jsonify({'error': 'Missing name or url'}), 400
    parsed = urllib.parse.urlparse(url_value)
    if parsed.scheme not in ('http', 'https'):
        return jsonify({'error': 'URL inválida'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO integrations_webhooks (name, url, events, is_active, created_at) VALUES (?, ?, ?, 1, ?)",
        (name, url_value, events_value, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('create_webhook', {'name': name, 'url': url_value})
    return jsonify({'success': True})

@app.route('/api/integrations/webhooks/<int:webhook_id>/delete', methods=['DELETE'])
@login_required
def api_webhooks_delete(webhook_id):
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("DELETE FROM integrations_webhooks WHERE id = ?", (webhook_id,))
    conn.commit()
    conn.close()
    log_admin_action('delete_webhook', {'id': webhook_id})
    return jsonify({'success': True})

@app.route('/api/integrations/webhooks/dispatch', methods=['POST'])
@login_required
def api_webhooks_dispatch():
    data = request.json or {}
    event = data.get('event')
    payload = data.get('payload', {})
    if not event:
        return jsonify({'error': 'Missing event'}), 400
    sent = dispatch_webhooks(event, payload)
    log_admin_action('dispatch_webhooks', {'event': event, 'sent': sent})
    return jsonify({'success': True, 'sent': sent})

@app.route('/api/optimization/metrics')
@login_required
def api_metrics():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT key, value, timestamp FROM metrics ORDER BY timestamp DESC LIMIT 100")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'key': r['key'],
        'value': r['value'],
        'timestamp': r['timestamp']
    } for r in rows])

@app.route('/api/optimization/metrics/record', methods=['POST'])
@login_required
def api_metrics_record():
    data = request.json or {}
    key = data.get('key')
    value = data.get('value')
    if not key:
        return jsonify({'error': 'Missing key'}), 400
    if value is not None and not isinstance(value, (int, float)):
        return jsonify({'error': 'Invalid value'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO metrics (key, value, timestamp) VALUES (?, ?, ?)",
        (key, value, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('metrics_record', {'key': key, 'value': value})
    return jsonify({'success': True})

@app.route('/api/optimization/backups')
@login_required
def api_backups():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute("SELECT path, size, status, created_at FROM backups ORDER BY created_at DESC LIMIT 50")
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'path': r['path'],
        'size': r['size'],
        'status': r['status'],
        'created_at': r['created_at']
    } for r in rows])

@app.route('/api/optimization/backups/record', methods=['POST'])
@login_required
def api_backups_record():
    data = request.json or {}
    path = data.get('path')
    size = data.get('size', 0)
    if not path:
        return jsonify({'error': 'Missing path'}), 400
    conn = get_db_connection()
    c = conn.cursor()
    c.execute(
        "INSERT INTO backups (path, size, status, created_at) VALUES (?, ?, 'created', ?)",
        (path, size, int(datetime.now().timestamp()))
    )
    conn.commit()
    conn.close()
    log_admin_action('backup_record', {'path': path, 'size': size})
    return jsonify({'success': True})

@app.route('/api/optimization/backups/run', methods=['POST'])
@login_required
def api_backups_run():
    backup_script = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'scripts', 'backup.sh'))
    if not os.path.exists(backup_script):
        return jsonify({'error': 'backup.sh not found'}), 404
    try:
        subprocess.run([backup_script], check=True)
        latest = None
        latest_size = 0
        if os.path.isdir(BACKUP_DIR):
            files = [os.path.join(BACKUP_DIR, f) for f in os.listdir(BACKUP_DIR)]
            files = [f for f in files if os.path.isfile(f)]
            if files:
                latest = max(files, key=os.path.getmtime)
                latest_size = os.path.getsize(latest)
        conn = get_db_connection()
        c = conn.cursor()
        c.execute(
            "INSERT INTO backups (path, size, status, created_at) VALUES (?, ?, 'created', ?)",
            (latest or 'backup', latest_size, int(datetime.now().timestamp()))
        )
        conn.commit()
        conn.close()
        log_admin_action('backup_run', {'path': latest, 'size': latest_size})
        return jsonify({'success': True, 'path': latest, 'size': latest_size})
    except subprocess.CalledProcessError as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/health')
def api_health():
    status = {'status': 'ok'}
    try:
        conn = get_db_connection()
        conn.execute('SELECT 1')
        conn.close()
        status['db'] = 'ok'
    except Exception as e:
        status['db'] = f'error: {e}'
        status['status'] = 'degraded'

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(2)
        sock.connect((RCON_HOST, RCON_PORT))
        sock.close()
        status['rcon'] = 'ok'
    except Exception:
        status['rcon'] = 'unavailable'
        status['status'] = 'degraded'

    try:
        usage = shutil.disk_usage('/')
        status['disk_free'] = usage.free
        status['disk_total'] = usage.total
    except Exception:
        status['disk_free'] = None
        status['disk_total'] = None

    return jsonify(status)

@app.route('/api/players')
@login_required
def api_players():
    filters = request.args.get('filters', '{}')
    try:
        filter_dict = json.loads(filters)
    except:
        filter_dict = {}

    cache_key = f"players:{json.dumps(filter_dict, sort_keys=True)}"
    cached = get_cache(cache_key)
    if cached is not None:
        return jsonify(cached)
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    query = "SELECT uuid, username, player_class, level, experience FROM players WHERE 1=1"
    params = []
    
    if 'min_level' in filter_dict:
        if not isinstance(filter_dict['min_level'], int):
            return jsonify({'error': 'min_level inválido'}), 400
        query += " AND level >= ?"
        params.append(filter_dict['min_level'])
    
    if 'max_level' in filter_dict:
        if not isinstance(filter_dict['max_level'], int):
            return jsonify({'error': 'max_level inválido'}), 400
        query += " AND level <= ?"
        params.append(filter_dict['max_level'])
    
    if 'class' in filter_dict:
        query += " AND player_class = ?"
        params.append(filter_dict['class'])
    
    query += " ORDER BY level DESC LIMIT 100"
    
    c.execute(query, params)
    players = c.fetchall()
    conn.close()
    
    result = [{
        'uuid': p[0],
        'username': p[1],
        'class': p[2],
        'level': p[3],
        'experience': p[4]
    } for p in players]

    set_cache(cache_key, result)
    return jsonify(result)

# ==================== PUBLIC API ====================

@app.route('/api/public/status')
def api_public_status():
    return jsonify({
        'status': 'online',
        'server_time': datetime.utcnow().isoformat() + 'Z'
    })

@app.route('/api/public/leaderboard')
def api_public_leaderboard():
    limit = request.args.get('limit', 10, type=int)
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT uuid, username, level, experience FROM players ORDER BY level DESC, experience DESC LIMIT ?", (limit,))
    rows = c.fetchall()
    conn.close()
    return jsonify([{
        'uuid': r[0],
        'username': r[1],
        'level': r[2],
        'experience': r[3]
    } for r in rows])

@app.route('/api/players/<uuid>')
@login_required
def api_player_details(uuid):
    if not is_valid_uuid(uuid):
        return jsonify({'error': 'UUID inválido'}), 400
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        SELECT uuid, username, player_class, level, experience, 
               health, max_health, mana, max_mana
        FROM players WHERE uuid = ?
    """, (uuid,))
    player = c.fetchone()
    conn.close()
    
    if not player:
        return jsonify({'error': 'Player not found'}), 404
    
    return jsonify({
        'uuid': player[0],
        'username': player[1],
        'class': player[2],
        'level': player[3],
        'experience': player[4],
        'health': player[5],
        'max_health': player[6],
        'mana': player[7],
        'max_mana': player[8]
    })

@app.route('/api/players/<uuid>/update', methods=['POST'])
@login_required
def api_update_player(uuid):
    if not is_valid_uuid(uuid):
        return jsonify({'error': 'UUID inválido'}), 400
    data = request.json or {}
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    update_fields = []
    params = []
    
    if 'level' in data:
        update_fields.append('level = ?')
        params.append(data['level'])
    
    if 'experience' in data:
        update_fields.append('experience = ?')
        params.append(data['experience'])
    
    if 'health' in data:
        update_fields.append('health = ?')
        params.append(data['health'])
    
    if 'mana' in data:
        update_fields.append('mana = ?')
        params.append(data['mana'])
    
    if not update_fields:
        return jsonify({'error': 'No fields to update'}), 400
    
    params.append(uuid)
    query = f"UPDATE players SET {', '.join(update_fields)} WHERE uuid = ?"
    
    c.execute(query, params)
    conn.commit()
    conn.close()

    log_admin_action('update_player', {'uuid': uuid, 'fields': update_fields})
    
    return jsonify({'success': True})

@app.route('/api/economy')
@login_required
def api_economy_stats():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    # Total coins in economy
    c.execute("SELECT COALESCE(SUM(balance), 0) FROM player_economy")
    total_coins = c.fetchone()[0]
    
    # Richest player
    c.execute("""
        SELECT p.username, pe.balance 
        FROM player_economy pe
        JOIN players p ON pe.player_uuid = p.uuid
        ORDER BY pe.balance DESC LIMIT 1
    """)
    richest = c.fetchone()
    
    # Recent transactions count
    c.execute("SELECT COUNT(*) FROM transactions WHERE timestamp >= datetime('now', '-24 hours')")
    recent_transactions = c.fetchone()[0]
    
    # Average balance
    c.execute("SELECT COALESCE(AVG(balance), 0) FROM player_economy")
    avg_balance = c.fetchone()[0]
    
    conn.close()
    
    return jsonify({
        'total_coins': total_coins,
        'richest_player': richest[0] if richest else 'N/A',
        'richest_balance': richest[1] if richest else 0,
        'recent_transactions_24h': recent_transactions,
        'average_balance': int(avg_balance)
    })

@app.route('/api/economy/transactions')
@login_required
def api_transactions():
    limit = request.args.get('limit', 50, type=int)
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        SELECT t.id, p.username, t.type, t.amount, t.description, t.timestamp
        FROM transactions t
        JOIN players p ON t.player_uuid = p.uuid
        ORDER BY t.timestamp DESC
        LIMIT ?
    """, (limit,))
    transactions = c.fetchall()
    conn.close()
    
    return jsonify([{
        'id': t[0],
        'player': t[1],
        'type': t[2],
        'amount': t[3],
        'description': t[4],
        'timestamp': t[5]
    } for t in transactions])

@app.route('/api/economy/add-coins', methods=['POST'])
@login_required
def api_add_coins():
    data = request.json
    uuid = data.get('uuid')
    amount = data.get('amount', 0)
    
    if not is_valid_uuid(uuid) or amount <= 0:
        return jsonify({'error': 'Invalid parameters'}), 400
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    # Get current balance
    c.execute("SELECT balance FROM player_economy WHERE player_uuid = ?", (uuid,))
    result = c.fetchone()
    
    if not result:
        return jsonify({'error': 'Player not found'}), 404
    
    old_balance = result[0]
    new_balance = old_balance + amount
    
    # Update balance
    c.execute("UPDATE player_economy SET balance = ? WHERE player_uuid = ?", (new_balance, uuid))
    
    # Log transaction
    c.execute("""
        INSERT INTO transactions (player_uuid, type, amount, description, balance_before, balance_after)
        VALUES (?, 'ADMIN_GIVE', ?, 'Admin added coins', ?, ?)
    """, (uuid, amount, old_balance, new_balance))
    
    conn.commit()
    conn.close()

    log_admin_action('add_coins', {'uuid': uuid, 'amount': amount})
    
    return jsonify({'success': True, 'new_balance': new_balance})

@app.route('/api/economy/remove-coins', methods=['POST'])
@login_required
def api_remove_coins():
    data = request.json
    uuid = data.get('uuid')
    amount = data.get('amount', 0)
    
    if not is_valid_uuid(uuid) or amount <= 0:
        return jsonify({'error': 'Invalid parameters'}), 400
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    c.execute("SELECT balance FROM player_economy WHERE player_uuid = ?", (uuid,))
    result = c.fetchone()
    
    if not result:
        return jsonify({'error': 'Player not found'}), 404
    
    old_balance = result[0]
    new_balance = max(0, old_balance - amount)
    
    c.execute("UPDATE player_economy SET balance = ? WHERE player_uuid = ?", (new_balance, uuid))
    
    c.execute("""
        INSERT INTO transactions (player_uuid, type, amount, description, balance_before, balance_after)
        VALUES (?, 'ADMIN_TAKE', ?, 'Admin removed coins', ?, ?)
    """, (uuid, amount, old_balance, new_balance))
    
    conn.commit()
    conn.close()

    log_admin_action('remove_coins', {'uuid': uuid, 'amount': amount})
    
    return jsonify({'success': True, 'new_balance': new_balance})

@app.route('/api/quests')
@login_required
def api_quests():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT id, name, description, required_level, rewards FROM quests ORDER BY required_level")
    quests = c.fetchall()
    conn.close()
    
    return jsonify([{
        'id': q[0],
        'name': q[1],
        'description': q[2],
        'required_level': q[3],
        'rewards': q[4]
    } for q in quests])

@app.route('/api/quests/create', methods=['POST'])
@login_required
def api_create_quest():
    data = request.json or {}
    if not data.get('name'):
        return jsonify({'error': 'Missing name'}), 400
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        INSERT INTO quests (name, description, required_level, objectives, rewards)
        VALUES (?, ?, ?, ?, ?)
    """, (
        data.get('name'),
        data.get('description'),
        data.get('required_level', 1),
        data.get('objectives', '[]'),
        data.get('rewards', '{}')
    ))
    conn.commit()
    quest_id = c.lastrowid
    conn.close()

    log_admin_action('create_quest', {'id': quest_id, 'name': data.get('name')})
    
    return jsonify({'success': True, 'id': quest_id})

@app.route('/api/quests/<int:quest_id>/update', methods=['POST'])
@login_required
def api_update_quest(quest_id):
    data = request.json or {}
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    update_fields = []
    params = []
    
    if 'name' in data:
        update_fields.append('name = ?')
        params.append(data['name'])
    
    if 'description' in data:
        update_fields.append('description = ?')
        params.append(data['description'])
    
    if 'required_level' in data:
        update_fields.append('required_level = ?')
        params.append(data['required_level'])
    
    if 'objectives' in data:
        update_fields.append('objectives = ?')
        params.append(json.dumps(data['objectives']))
    
    if 'rewards' in data:
        update_fields.append('rewards = ?')
        params.append(json.dumps(data['rewards']))
    
    if update_fields:
        params.append(quest_id)
        query = f"UPDATE quests SET {', '.join(update_fields)} WHERE id = ?"
        c.execute(query, params)
        conn.commit()
    
    conn.close()

    log_admin_action('update_quest', {'id': quest_id, 'fields': update_fields})
    
    return jsonify({'success': True})

@app.route('/api/quests/<int:quest_id>', methods=['DELETE'])
@login_required
def api_delete_quest(quest_id):
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("DELETE FROM quests WHERE id = ?", (quest_id,))
    conn.commit()
    conn.close()

    log_admin_action('delete_quest', {'id': quest_id})
    
    return jsonify({'success': True})

@app.route('/api/mobs')
@login_required
def api_mobs():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT id, name, type, health, damage, level FROM custom_mobs ORDER BY level")
    mobs = c.fetchall()
    conn.close()
    
    return jsonify([{
        'id': m[0],
        'name': m[1],
        'type': m[2],
        'health': m[3],
        'damage': m[4],
        'level': m[5]
    } for m in mobs])

@app.route('/api/mobs/<int:mob_id>/update', methods=['POST'])
@login_required
def api_update_mob(mob_id):
    data = request.json or {}
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    update_fields = []
    params = []
    
    for field in ['name', 'health', 'damage', 'level', 'drops', 'abilities']:
        if field in data:
            update_fields.append(f'{field} = ?')
            params.append(data[field] if field not in ['drops', 'abilities'] else json.dumps(data[field]))
    
    if update_fields:
        params.append(mob_id)
        query = f"UPDATE custom_mobs SET {', '.join(update_fields)} WHERE id = ?"
        c.execute(query, params)
        conn.commit()
    
    conn.close()

    log_admin_action('update_mob', {'id': mob_id, 'fields': update_fields})
    
    return jsonify({'success': True})

# RCON Helper Functions
def send_rcon_command(command):
    """Sends a command via RCON"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(10)
    
    try:
        sock.connect((RCON_HOST, RCON_PORT))
        
        # Authenticate
        auth_packet = create_rcon_packet(0, 3, RCON_PASSWORD)
        sock.sendall(auth_packet)
        auth_response = sock.recv(4096)
        
        # Send command
        cmd_packet = create_rcon_packet(1, 2, command)
        sock.sendall(cmd_packet)
        response = sock.recv(4096)
        
        # Parse response
        return parse_rcon_packet(response)
    finally:
        sock.close()

def create_rcon_packet(req_id, packet_type, payload):
    """Creates an RCON packet"""
    payload_bytes = payload.encode('utf-8')
    length = len(payload_bytes) + 10
    return struct.pack('<iii', length, req_id, packet_type) + payload_bytes + b'\x00\x00'

def parse_rcon_packet(data):
    """Parses an RCON packet response"""
    if len(data) < 12:
        return ""
    payload = data[12:-2].decode('utf-8', errors='ignore')
    return payload

def get_server_uptime():
    """Gets server uptime in seconds"""
    try:
        result = subprocess.run(['systemctl', 'show', 'minecraft-server', '-p', 'ActiveEnterTimestamp'],
                              capture_output=True, text=True, timeout=5)
        timestamp_str = result.stdout.split('=')[1].strip()
        start_time = datetime.strptime(timestamp_str, '%a %Y-%m-%d %H:%M:%S %Z')
        uptime = (datetime.now() - start_time).total_seconds()
        return int(uptime)
    except:
        return 0

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
