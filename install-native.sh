#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Minecraft MMORPG System - Native Installation Script
# ═══════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="$SCRIPT_DIR/server"
JAVA_VERSION="21"
PAPER_VERSION="1.20.6"
PAPER_BUILD="151"

echo "═══════════════════════════════════════════════════════════════"
echo "  Minecraft MMORPG System - Installer"
echo "  Version: 1.0.0"
echo "═══════════════════════════════════════════════════════════════"

confirm() {
    while true; do
        read -r -p "$1 (y/n): " yn
        case $yn in
            [Yy]* ) return 0;;
            [Nn]* ) return 1;;
            * ) echo "Please answer y or n.";;
        esac
    done
}

# Check Java
echo "[1/8] Checking Java $JAVA_VERSION..."
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "version \"$JAVA_VERSION"; then
    echo "Java $JAVA_VERSION not found. Installing..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sudo apt update
        sudo apt install -y openjdk-21-jdk
    else
        echo "Please install Java $JAVA_VERSION manually"
        exit 1
    fi
fi

# Check Maven
echo "[2/8] Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Installing..."
    sudo apt install -y maven
fi

# Download Paper
echo "[3/8] Downloading Paper $PAPER_VERSION..."
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"
if [ ! -f "paper-$PAPER_VERSION-$PAPER_BUILD.jar" ]; then
    wget "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar"
fi

# Accept EULA
echo "eula=true" > eula.txt

# Build plugin
echo "[4/8] Building MMORPG plugin..."
cd "$SCRIPT_DIR/mmorpg-plugin"
mvn clean package -DskipTests
mkdir -p "$INSTALL_DIR/plugins"
cp target/mmorpg-plugin-*.jar "$INSTALL_DIR/plugins/"

# Copy configs
echo "[5/8] Copying configuration files..."
mkdir -p "$INSTALL_DIR/config"
cp -r "$SCRIPT_DIR/config/"* "$INSTALL_DIR/config/"

# Create active world symlink if default world exists
if [ -d "$INSTALL_DIR/worlds/mundo-inicial" ]; then
    ln -sfn "$INSTALL_DIR/worlds/mundo-inicial" "$INSTALL_DIR/worlds/active"
fi

# Setup Python environment
echo "[6/8] Setting up Python environment for web panel..."
if [ ! -d "$INSTALL_DIR/web" ]; then
    cp -r "$SCRIPT_DIR/web" "$INSTALL_DIR/"
fi
cd "$INSTALL_DIR/web"
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Create management scripts in server root
echo "[7/8] Creating management scripts..."
cat > "$INSTALL_DIR/start-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl start mmorpg-server.service
else
    cd "$(dirname "$0")"
    exec /usr/bin/java -Xms4G -Xmx4G -jar paper-1.20.6-151.jar nogui
fi
EOF

cat > "$INSTALL_DIR/stop-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl stop mmorpg-server.service
else
    pkill -f "paper.*jar" || true
fi
EOF

cat > "$INSTALL_DIR/restart-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl restart mmorpg-server.service
else
    pkill -f "paper.*jar" || true
    cd "$(dirname "$0")"
    exec /usr/bin/java -Xms4G -Xmx4G -jar paper-1.20.6-151.jar nogui
fi
EOF

cat > "$INSTALL_DIR/logs-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    journalctl -u mmorpg-server.service -f --no-pager
else
    tail -f "$(dirname "$0")/logs/latest.log"
fi
EOF

cat > "$INSTALL_DIR/status-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    systemctl status mmorpg-server.service --no-pager
else
    pgrep -f "paper.*jar" >/dev/null && echo "Server: running" || echo "Server: stopped"
fi
EOF

cat > "$INSTALL_DIR/start-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    sudo systemctl start mmorpg-web.service
else
    cd "$(dirname "$0")/web"
    exec ./start-web.sh
fi
EOF

cat > "$INSTALL_DIR/stop-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    sudo systemctl stop mmorpg-web.service
else
    pkill -f "flask.*app.py" || true
fi
EOF

cat > "$INSTALL_DIR/restart-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    sudo systemctl restart mmorpg-web.service
else
    pkill -f "flask.*app.py" || true
    cd "$(dirname "$0")/web"
    exec ./start-web.sh
fi
EOF

cat > "$INSTALL_DIR/logs-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    journalctl -u mmorpg-web.service -f --no-pager
else
    tail -f "$(dirname "$0")/web/panel.log"
fi
EOF

cat > "$INSTALL_DIR/status-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    systemctl status mmorpg-web.service --no-pager
else
    pgrep -f "flask.*app.py" >/dev/null && echo "Web: running" || echo "Web: stopped"
fi
EOF

chmod +x \
    "$INSTALL_DIR"/start-server.sh \
    "$INSTALL_DIR"/stop-server.sh \
    "$INSTALL_DIR"/restart-server.sh \
    "$INSTALL_DIR"/logs-server.sh \
    "$INSTALL_DIR"/status-server.sh \
    "$INSTALL_DIR"/start-web.sh \
    "$INSTALL_DIR"/stop-web.sh \
    "$INSTALL_DIR"/restart-web.sh \
    "$INSTALL_DIR"/logs-web.sh \
    "$INSTALL_DIR"/status-web.sh

# Create systemd services (optional)
echo "[8/8] System services (optional)..."
if confirm "Install Minecraft server as a system service?"; then
    sudo tee /etc/systemd/system/mmorpg-server.service > /dev/null <<EOF
[Unit]
Description=Minecraft MMORPG Server
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -Xms4G -Xmx4G -jar paper-$PAPER_VERSION-$PAPER_BUILD.jar nogui
Restart=always

[Install]
WantedBy=multi-user.target
EOF
    sudo systemctl daemon-reload
    sudo systemctl start mmorpg-server
    sudo systemctl enable mmorpg-server
fi

if confirm "Install web panel as a system service?"; then
    sudo tee /etc/systemd/system/mmorpg-web.service > /dev/null <<EOF
[Unit]
Description=MMORPG Web Panel
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=$INSTALL_DIR/web
ExecStart=$INSTALL_DIR/web/venv/bin/python app.py
Restart=always

[Install]
WantedBy=multi-user.target
EOF
    sudo systemctl daemon-reload
    sudo systemctl start mmorpg-web
    sudo systemctl enable mmorpg-web
fi

echo "═══════════════════════════════════════════════════════════════"
echo "  Installation Complete!"
echo "  Server: localhost:25565"
echo "  Web Panel: http://localhost:5000"
echo "  Default credentials: admin/admin"
echo "═══════════════════════════════════════════════════════════════"
