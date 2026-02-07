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

# Setup Python environment
echo "[6/8] Setting up Python environment for web panel..."
if [ ! -d "$INSTALL_DIR/web" ]; then
    cp -r "$SCRIPT_DIR/web" "$INSTALL_DIR/"
fi
cd "$INSTALL_DIR/web"
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt

# Create systemd services
echo "[7/8] Creating systemd services..."
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

echo "[8/8] Starting services..."
sudo systemctl start mmorpg-server
sudo systemctl enable mmorpg-server
sudo systemctl start mmorpg-web
sudo systemctl enable mmorpg-web

echo "═══════════════════════════════════════════════════════════════"
echo "  Installation Complete!"
echo "  Server: localhost:25565"
echo "  Web Panel: http://localhost:5000"
echo "  Default credentials: admin/admin"
echo "═══════════════════════════════════════════════════════════════"
