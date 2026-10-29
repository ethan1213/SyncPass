import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import {VitePWA} from 'vite-plugin-pwa';
export default defineConfig({
 plugins:[react(),VitePWA({registerType:'autoUpdate',injectRegister:'auto',
  manifest:{name:'SyncPass',short_name:'SyncPass',start_url:'/',display:'standalone',background_color:'#ffffff',theme_color:'#333333'},
  workbox:{globPatterns:['**/*.{js,css,html,ico,svg}'],navigateFallbackDenylist:[/^\/api\//,/^\/auth\//],maximumFileSizeToCacheInBytes:3*1024*1024}})],
 server:{port:3000,proxy:{'/api':'http://localhost:8080'},open:false}
});
