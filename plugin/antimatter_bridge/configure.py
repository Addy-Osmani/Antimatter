import json
from pathlib import Path

CONFIG_FILE = Path.home() / ".antimatter_daemon" / "config.json"

def main():
    print("="*50)
    print("ANTIMATTER BRIDGE CONFIGURATION")
    print("="*50)
    print("\nThis script helps you configure Cloudflare Zero Trust settings.")
    
    config = {}
    if CONFIG_FILE.exists():
        with open(CONFIG_FILE, "r") as f:
            config = json.load(f)

    print(f"\nCurrent URL: {config.get('cloudflare_url', 'Not set')}")
    url = input("Enter Cloudflare Tunnel URL (or press Enter to skip): ").strip()
    if url:
        config["cloudflare_url"] = url

    print(f"\nCurrent Client ID: {config.get('cloudflare_client_id', 'Not set')}")
    client_id = input("Enter Cloudflare Access Client ID (or press Enter to skip): ").strip()
    if client_id:
        config["cloudflare_client_id"] = client_id

    secret = config.get('cloudflare_client_secret')
    status = "(Set)" if secret else "(Not set)"
    print(f"\nCurrent Client Secret: {status}")
    client_secret = input("Enter Cloudflare Access Client Secret (or press Enter to skip): ").strip()
    if client_secret:
        config["cloudflare_client_secret"] = client_secret

    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=4)

    print("\nConfiguration saved successfully!")
    print("Restart the Antimatter daemon to apply these settings and generate your new QR code.")

if __name__ == "__main__":
    main()
