module.exports = {
  window: {
    showWarningMessage: jest.fn(),
    showErrorMessage: jest.fn(),
    showInformationMessage: jest.fn(),
  },
  workspace: {
    workspaceFolders: [
      { uri: { fsPath: '/test/workspace/path' } }
    ],
  },
  ViewColumn: {
    One: 1
  }
};
