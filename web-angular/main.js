const { app, BrowserWindow } = require('electron');
const path = require('path');

let win;

function createWindow() {
  win = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: true, // Permite usar Node.js en la app
      contextIsolation: false
    }
  });

  // IMPORTANTE: Ajusta esta ruta a tu carpeta de build
  win.loadFile(path.join(__dirname, 'dist/web-angular/browser/index.html'));

  win.on('closed', () => {
    win = null;
  });
}

app.on('ready', createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});