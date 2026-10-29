import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import {SessionProvider} from './auth/SessionProvider';
import './index.css';
const root = document.getElementById('root');
if (!root) throw new Error('No se encontró el contenedor de SyncPass.');
ReactDOM.createRoot(root).render(<React.StrictMode><SessionProvider><App/></SessionProvider></React.StrictMode>);
