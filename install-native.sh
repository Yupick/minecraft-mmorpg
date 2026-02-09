#!/bin/bash

# ═══════════════════════════════════════════════════════════════
# Minecraft MMORPG System - Native Installation Script  
# ═══════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INSTALL_DIR="$SCRIPT_DIR/server"
JAVA_VERSION="21"
# Paper version will be selected interactively
PAPER_VERSION=""
PAPER_BUILD=""

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

# Helper function to download with curl (follows redirects)
download_file() {
    local url="$1"
    local output="$2"
    if command -v curl &> /dev/null; then
        curl -fSL -o "$output" "$url"
    elif command -v wget &> /dev/null; then
        wget -qO "$output" "$url"
    else
        echo "ERROR: Neither curl nor wget found"
        return 1
    fi
}

# Helper function to download from Hangar
download_from_hangar() {
    local project_slug="$1"
    local output="$2"
    local platform="${3:-PAPER}"
    
    # Get latest version info with download URL or external URL
    local download_info=$(curl -sL "https://hangar.papermc.io/api/v1/projects/${project_slug}/versions?limit=1&offset=0" 2>/dev/null | \
        python3 -c "import json,sys; data=json.load(sys.stdin); dl=data['result'][0]['downloads']['${platform}'] if data.get('result') and len(data['result']) > 0 else {}; print(dl.get('downloadUrl') or dl.get('externalUrl') or '')" 2>/dev/null)
    
    if [ -z "$download_info" ]; then
        return 1
    fi
    
    # Download (suppress curl progress but show errors)
    download_file "$download_info" "$output" 2>&1 | grep -v "Total\|Dload\|%" || true
    
    # Validate downloaded file
    if [ -f "$output" ] && [ -s "$output" ]; then
        return 0
    else
        rm -f "$output" 2>/dev/null
        return 1
    fi
}

# Select Paper version interactively
echo ""
echo "Fetching available Paper versions..."
PAPER_VERSIONS=$(curl -sL "https://api.papermc.io/v2/projects/paper" 2>/dev/null | python3 -c "import json,sys; data=json.load(sys.stdin); print(' '.join(data['versions'][-15:]))" 2>/dev/null)

if [ -z "$PAPER_VERSIONS" ]; then
    echo "⚠ Could not fetch versions from API, using default: 1.20.6"
    PAPER_VERSION="1.20.6"
    PAPER_BUILD="151"
else
    echo ""
    echo "Available Paper versions (last 15):"
    i=1
    for v in $PAPER_VERSIONS; do
        printf "  %2d. %s\n" "$i" "$v"
        ((i++))
    done
    echo ""
    read -p "Enter Paper version [default: 1.20.6]: " selected_version
    PAPER_VERSION="${selected_version:-1.20.6}"
    
    echo "Fetching latest build for Paper $PAPER_VERSION..."
    PAPER_BUILD=$(curl -sL "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION" 2>/dev/null | python3 -c "import json,sys; data=json.load(sys.stdin); print(data['builds'][-1])" 2>/dev/null)
    
    if [ -z "$PAPER_BUILD" ]; then
        echo "ERROR: Could not fetch build for version $PAPER_VERSION"
        exit 1
    fi
    
    echo "✓ Using Paper $PAPER_VERSION build $PAPER_BUILD"
fi
echo ""

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
echo "[3/8] Downloading Paper $PAPER_VERSION build $PAPER_BUILD..."
mkdir -p "$INSTALL_DIR"
cd "$INSTALL_DIR"
if [ ! -f "paper-$PAPER_VERSION-$PAPER_BUILD.jar" ]; then
    echo "  Downloading from PaperMC API..."
    download_file "https://api.papermc.io/v2/projects/paper/versions/$PAPER_VERSION/builds/$PAPER_BUILD/downloads/paper-$PAPER_VERSION-$PAPER_BUILD.jar" "paper-$PAPER_VERSION-$PAPER_BUILD.jar" || {
        echo "ERROR: Failed to download Paper"
        exit 1
    }
    echo "  ✓ Downloaded $(du -h paper-$PAPER_VERSION-$PAPER_BUILD.jar | cut -f1)"
else
    echo "  ✓ Already exists"
fi

# Accept EULA
echo "eula=true" > eula.txt

# Build plugin
echo "[4/8] Building MMORPG plugin..."
cd "$SCRIPT_DIR/mmorpg-plugin"
mvn clean package -DskipTests
mkdir -p "$INSTALL_DIR/plugins"
cp target/mmorpg-plugin-*.jar "$INSTALL_DIR/plugins/"

# Download compatibility plugins
echo "[4.5/8] Downloading compatibility plugins..."

# Helper function to validate JAR file
validate_jar() {
    if [ -f "$1" ] && [ -s "$1" ]; then
        file "$1" | grep -q "Java\|Zip" && return 0
        rm -f "$1"
    fi
    return 1
}

# Geyser-Spigot (Bedrock Edition support)
echo "  • Geyser-Spigot (Bedrock Edition support)..."
if download_from_hangar "GeyserMC/Geyser" "$INSTALL_DIR/plugins/Geyser-Spigot.jar" "PAPER"; then
    echo "    ✓ Downloaded from Hangar"
else
    echo "    ⚠ Hangar failed, trying direct download..."
    if download_file "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" "$INSTALL_DIR/plugins/Geyser-Spigot.jar" 2>&1 | grep -v "%" && validate_jar "$INSTALL_DIR/plugins/Geyser-Spigot.jar"; then
        echo "    ✓ Downloaded from GeyserMC"
    else
        echo "    ✗ FAILED (optional)"
    fi
fi

# Floodgate-Spigot (Bedrock authentication)
echo "  • Floodgate-Spigot (Bedrock authentication)..."
if download_from_hangar "GeyserMC/Floodgate" "$INSTALL_DIR/plugins/floodgate-spigot.jar" "PAPER"; then
    echo "    ✓ Downloaded from Hangar"
else
    echo "    ⚠ Hangar failed, trying direct download..."
    if download_file "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" "$INSTALL_DIR/plugins/floodgate-spigot.jar" 2>&1 | grep -v "%" && validate_jar "$INSTALL_DIR/plugins/floodgate-spigot.jar"; then
        echo "    ✓ Downloaded from GeyserMC"
    else
        echo "    ✗ FAILED (optional)"
    fi
fi

# ViaVersion (Support for older Java Edition versions)
echo "  • ViaVersion (Java Edition version compatibility)..."
if download_from_hangar "ViaVersion/ViaVersion" "$INSTALL_DIR/plugins/ViaVersion.jar" "PAPER"; then
    echo "    ✓ Downloaded from Hangar"
else
    echo "    ✗ FAILED (optional)"
fi

# ViaBackwards (Support for older versions compatibility)
echo "  • ViaBackwards (Older version support)..."
if download_from_hangar "ViaVersion/ViaBackwards" "$INSTALL_DIR/plugins/ViaBackwards.jar" "PAPER"; then
    echo "    ✓ Downloaded from Hangar"
else
    echo "    ✗ FAILED (optional)"
fi

# ViaRewind (Support for very old versions)
echo "  • ViaRewind (Very old version support)..."
if download_from_hangar "ViaVersion/ViaRewind" "$INSTALL_DIR/plugins/ViaRewind.jar" "PAPER"; then
    echo "    ✓ Downloaded from Hangar"
else
    echo "    ✗ FAILED (optional)"
fi

# List downloaded plugins
echo ""
echo "  Downloaded plugins:"
ls -1 "$INSTALL_DIR/plugins/"*.jar 2>/dev/null | while read jar; do
    size=$(du -h "$jar" | cut -f1)
    name=$(basename "$jar")
    printf "    ✓ %-30s %s\n" "$name" "$size"
done
echo ""

# Copy configs
echo "[5/8] Copying configuration files..."
mkdir -p "$INSTALL_DIR/config"
cp -r "$SCRIPT_DIR/config/"* "$INSTALL_DIR/config/"

# Create active world symlink if default world exists
if [  -d "$INSTALL_DIR/worlds/mundo-inicial" ]; then
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
cat > "$INSTALL_DIR/start-server.sh" <<EOF
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl start mmorpg-server.service
else
    cd "\$(dirname "\$0")"
    exec /usr/bin/java -Xms4G -Xmx4G -jar paper-$PAPER_VERSION-$PAPER_BUILD.jar nogui
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

cat > "$INSTALL_DIR/restart-server.sh" <<EOF
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    sudo systemctl restart mmorpg-server.service
else
    pkill -f "paper.*jar" || true
    cd "\$(dirname "\$0")"
    exec /usr/bin/java -Xms4G -Xmx4G -jar paper-$PAPER_VERSION-$PAPER_BUILD.jar nogui
fi
EOF

cat > "$INSTALL_DIR/logs-server.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-server.service"; then
    journalctl -u mmorpg-server.service -f --no-pager
else
    tail -f "\$(dirname "\$0")/logs/latest.log"
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
    cd "\$(dirname "\$0")/web"
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
    cd "\$(dirname "\$0")/web"
    exec ./start-web.sh
fi
EOF

cat > "$INSTALL_DIR/logs-web.sh" <<'EOF'
#!/bin/bash
set -e
if systemctl --version &> /dev/null && systemctl list-unit-files | grep -q "mmorpg-web.service"; then
    journalctl -u mmorpg-web.service -f --no-pager
else
    tail -f "\$(dirname "\$0")/web/panel.log"
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

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  Installation Complete!"
echo "  Paper Version: $PAPER_VERSION build $PAPER_BUILD"
echo "  Server: localhost:25565"
echo "  Web Panel: http://localhost:5000"
echo "  Default credentials: admin/admin"
echo "═══════════════════════════════════════════════════════════════"

