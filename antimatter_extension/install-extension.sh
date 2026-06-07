#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# install-extension.sh
# Installs the Antimatter Bridge extension into your local Antigravity IDE
# ─────────────────────────────────────────────────────────────────────────────

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXT_DIR="$HOME/.antigravity-ide/extensions/antimatter-bridge"

echo "🔨 Building Antimatter Bridge extension..."
cd "$SCRIPT_DIR"
node build.mjs

echo "📦 Installing to: $EXT_DIR"
rm -rf "$EXT_DIR"
mkdir -p "$EXT_DIR"

# Copy everything the extension needs
cp -r dist package.json "$EXT_DIR/"

echo ""
echo "✅ Installation complete!"
echo ""
echo "👉 Next steps:"
echo "   1. Restart Antigravity IDE"
echo "   2. The bridge starts automatically on port 8765"
echo "   3. Check the status bar for the Antimatter broadcast icon"
echo "   4. Open 'View → Output → Antimatter Bridge' for logs"
echo ""
echo "📱 On your Android phone:"
echo "   - On same Wi-Fi: the app will auto-discover this PC"
echo "   - On mobile data: enable Cloudflare Tunnel in settings"
