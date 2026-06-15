import asyncio
import os
import ptyprocess
import base64
import logging

logger = logging.getLogger(__name__)

class PtyManager:
    def __init__(self, router):
        self.router = router
        self.sessions = {}  # Map of id to ptyprocess.PtyProcess
        self.read_tasks = {}

    async def start_pty(self, session_id: str, cols: int = 80, rows: int = 24):
        if session_id in self.sessions:
            logger.info(f"PTY session {session_id} already exists, skipping start.")
            # Optionally resize to requested dimensions
            self.sessions[session_id].setwinsize(rows, cols)
            return

        try:
            # Spawn a raw byte-oriented pty
            pty = ptyprocess.PtyProcess.spawn(['/bin/bash'], dimensions=(rows, cols))
            self.sessions[session_id] = pty
            logger.info(f"Started PTY session {session_id} with PID {pty.pid}")

            # Make the file descriptor non-blocking
            os.set_blocking(pty.fd, False)

            # Start reading from it
            loop = asyncio.get_running_loop()
            
            async def _read_loop():
                queue = asyncio.Queue(maxsize=1000)
                
                def _reader():
                    try:
                        data = os.read(pty.fd, 4096)
                        if data:
                            try:
                                queue.put_nowait(data)
                            except asyncio.QueueFull:
                                logger.warning(f"Backpressure! Dropping output frames for {session_id}")
                    except BlockingIOError:
                        pass
                    except OSError as e:
                        logger.error(f"PTY read error for {session_id}: {e}")
                        loop.remove_reader(pty.fd)
                        self._cleanup(session_id)

                loop.add_reader(pty.fd, _reader)

                try:
                    while session_id in self.sessions:
                        data = await queue.get()
                        b64_data = base64.b64encode(data).decode('utf-8')
                        payload = {
                            "type": "PTY_OUTPUT",
                            "data": b64_data
                        }
                        if self.router.gateway and self.router.gateway.e2ee:
                            await self.router.broadcast_to_clients(payload, self.router.gateway.e2ee)
                except asyncio.CancelledError:
                    pass
                finally:
                    loop.remove_reader(pty.fd)

            self.read_tasks[session_id] = asyncio.create_task(_read_loop())

        except Exception as e:
            logger.error(f"Failed to start PTY for {session_id}: {e}")

    def write_input(self, session_id: str, data_b64: str):
        if session_id not in self.sessions:
            return
        
        try:
            data = base64.b64decode(data_b64)
            self.sessions[session_id].write(data)
        except Exception as e:
            logger.error(f"Failed to write input to {session_id}: {e}")

    def resize(self, session_id: str, cols: int, rows: int):
        if session_id not in self.sessions:
            return
        try:
            self.sessions[session_id].setwinsize(rows, cols)
        except Exception as e:
            logger.error(f"Failed to resize PTY {session_id}: {e}")

    def ping(self, session_id: str):
        if session_id not in self.sessions:
            return
        # Just an acknowledgment or keepalive logic if needed
        logger.debug(f"Received PING for {session_id}")

    def _cleanup(self, session_id: str):
        logger.info(f"Cleaning up PTY session {session_id}")
        if session_id in self.sessions:
            try:
                self.sessions[session_id].terminate(force=True)
            except:
                pass
            del self.sessions[session_id]
        if session_id in self.read_tasks:
            self.read_tasks[session_id].cancel()
            del self.read_tasks[session_id]

