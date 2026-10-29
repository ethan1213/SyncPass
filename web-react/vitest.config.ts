import { defineConfig } from 'vitest/config';
export default defineConfig({
  test: {
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      include: ['src/data/participantQr.ts', 'src/clients/api.ts', 'src/data/localDb.js', 'src/components/GeofenceNote.tsx'],
      reporter: ['text', 'html', 'json-summary'],
      thresholds: { lines: 80, functions: 80, branches: 80, statements: 80 }
    }
  }
});
