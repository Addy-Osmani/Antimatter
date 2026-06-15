import asyncio
import re
import logging

logger = logging.getLogger(__name__)

class CloudflaredManager:
    def __init__(self, port: int):
        self.port = port
        self.process = None
        self.url = None
        self.ready_event = asyncio.Event()

    async def start(self):
        try:
            self.process = await asyncio.create_subprocess_exec(
                "cloudflared", "tunnel", "--url", f"http://127.0.0.1:{self.port}",
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            
            # Cloudflared prints to stderr
            asyncio.create_task(self._read_stderr())
        except FileNotFoundError:
            logger.error("cloudflared CLI not found. Please install it to use Quick Tunnels.")
            return False
        return True

    async def _read_stderr(self):
        url_pattern = re.compile(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com")
        while True:
            line = await self.process.stderr.readline()
            if not line:
                break
            line_str = line.decode('utf-8').strip()
            
            logger.info(f"[Cloudflared] {line_str}")

            if not self.url:
                match = url_pattern.search(line_str)
                if match:
                    # Convert https to wss
                    self.url = match.group(0).replace("https://", "wss://")
                    self.ready_event.set()

    async def stop(self):
        if self.process:
            self.process.terminate()
            try:
                await asyncio.wait_for(self.process.wait(), timeout=5.0)
            except asyncio.TimeoutError:
                self.process.kill()
