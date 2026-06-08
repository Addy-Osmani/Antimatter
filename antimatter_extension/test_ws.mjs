import WebSocket from 'ws';
const ws = new WebSocket('ws://127.0.0.1:8765/?token=antimatter-super-secret-token');
ws.on('open', () => {
  ws.send(JSON.stringify({ type: 'GET_COMMANDS' }));
  setTimeout(() => process.exit(0), 1000);
});
