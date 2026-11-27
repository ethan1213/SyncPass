import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { render } from 'vitest-browser-react';
import { Route, Router, Switch } from 'wouter';
import { memoryLocation } from 'wouter/memory-location';
import { SessionProvider } from '../src/auth/SessionProvider';
import RequireAuth from '../src/components/RequireAuth';
import LoginPage from '../src/pages/LoginPage';

beforeEach(() => sessionStorage.removeItem('syncpass.session'));
afterEach(() => {
  vi.unstubAllGlobals();
  sessionStorage.removeItem('syncpass.session');
});

function montar(path: string) {
  return render(
    <SessionProvider>
      <Router hook={memoryLocation({ path }).hook}>
        <Switch>
          <Route path="/login"><LoginPage /></Route>
          <Route path="/mi-panel">
            <RequireAuth roles={['coordinador']}><h1>Panel de gestión</h1></RequireAuth>
          </Route>
        </Switch>
      </Router>
    </SessionProvider>,
  );
}

it('mantiene privada la ruta si no hay sesión', async () => {
  const pantalla = await montar('/mi-panel');
  await expect.element(pantalla.getByRole('heading', { name: 'Necesitas iniciar sesión' })).toBeVisible();
});

it('inicia sesión desde el formulario y abre la vista autorizada', async () => {
  const claims = btoa(JSON.stringify({
    sub: 'persona-de-prueba', email: 'demo@example.invalid', 'cognito:groups': ['coordinador'],
  }));
  const fetchSimulado = vi.fn(async (url: string) => {
    expect(url).toBe('https://example.invalid/api/auth/login');
    return Response.json({ accessToken: 'demo-access', idToken: `test.${claims}.sig`, refreshToken: 'demo-refresh', expiresIn: 3600 });
  });
  vi.stubGlobal('fetch', fetchSimulado);

  const pantalla = await montar('/login');
  await pantalla.getByLabelText('Usuario').fill('demo');
  await pantalla.getByLabelText('Contraseña').fill('clave-ficticia');
  await pantalla.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect.element(pantalla.getByRole('status')).toHaveTextContent('Sesión iniciada correctamente');
  await pantalla.getByRole('link', { name: 'Ir a mi panel' }).click();
  await expect.element(pantalla.getByRole('heading', { name: 'Panel de gestión' })).toBeVisible();
  expect(fetchSimulado).toHaveBeenCalledOnce();
});
