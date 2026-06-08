import * as vscode from 'vscode';
// Just a dummy test to see if compilation would work
const tracker: vscode.DebugAdapterTracker = {
    onDidSendMessage(msg: any) {
        if (msg.type === 'event' && msg.event === 'output' && msg.body && msg.body.output) {
            console.log(msg.body.output);
        }
    }
};
