import os
import sys
import shutil
import argparse
from pathlib import Path
from antimatter_bridge import server

def init_plugin():
    """Initializes the plugin by copying assets into the Antigravity IDE plugins directory."""
    print("Initializing Antimatter Bridge plugin...")
    
    home_dir = Path.home()
    plugin_dir = home_dir / ".gemini" / "config" / "plugins" / "antimatter-bridge"
    
    # Define source assets path
    assets_dir = Path(__file__).parent / "assets"
    
    if not assets_dir.exists():
        print(f"Error: Could not find assets directory at {assets_dir}", file=sys.stderr)
        sys.exit(1)
        
    try:
        # Create target directories
        print(f"Creating directory: {plugin_dir}")
        plugin_dir.mkdir(parents=True, exist_ok=True)
        
        # 1. Copy plugin.json
        print("Copying plugin.json...")
        shutil.copy2(assets_dir / "plugin.json", plugin_dir / "plugin.json")
        
        # 2. Create skills directory and copy SKILL.md
        skill_dir = plugin_dir / "skills" / "antimatter-bridge"
        print(f"Creating directory: {skill_dir}")
        skill_dir.mkdir(parents=True, exist_ok=True)
        
        print("Copying SKILL.md...")
        shutil.copy2(assets_dir / "skills" / "antimatter-bridge" / "SKILL.md", skill_dir / "SKILL.md")
        
        print("\n✅ Successfully initialized Antimatter Bridge!")
        print("You can now open Antigravity 2.0 and say: 'Start my Antimatter bridge'")
        
    except Exception as e:
        print(f"Failed to initialize plugin: {e}", file=sys.stderr)
        sys.exit(1)

def start_server():
    """Starts the WebSocket bridge adapter client."""
    print("Starting Antimatter Bridge Adapter...")
    try:
        import asyncio
        asyncio.run(server.main())
    except KeyboardInterrupt:
        print("\nAdapter stopped.")

def main():
    parser = argparse.ArgumentParser(description="Antimatter Bridge CLI")
    subparsers = parser.add_subparsers(dest="command", help="Available commands")
    
    # Init command
    init_parser = subparsers.add_parser("init", help="Initialize the plugin in the Antigravity SDK directory")
    
    # Start command
    start_parser = subparsers.add_parser("start", help="Start the WebSocket daemon")
    
    args = parser.parse_args()
    
    if args.command == "init":
        init_plugin()
    elif args.command == "start":
        start_server()
    else:
        parser.print_help()
        sys.exit(1)

if __name__ == "__main__":
    main()
