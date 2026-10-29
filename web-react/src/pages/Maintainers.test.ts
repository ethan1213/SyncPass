import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ProyectosAdminPage from './ProyectosAdminPage';
import TalleresAdminPage from './TalleresAdminPage';
import { request } from '../clients/api';

vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ getAccessTokenSilently: async () => 'token-demo' }) }));
vi.mock('../clients/api', () => ({ request: vi.fn() }));
const api = vi.mocked(request);
beforeEach(() => { api.mockReset(); vi.stubGlobal('confirm', vi.fn(() => true)); });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

describe('mantenedor de proyectos', () => {
  it('crea y vuelve a consultar el proyecto mediante la API', async () => {
    api.mockImplementation(async (path, options) => path === '/api/proyectos' && options?.method === 'POST'
      ? { id: 1 } : []);
    render(React.createElement(ProyectosAdminPage));
    fireEvent.change(screen.getByLabelText('Nombre del proyecto'), { target: { value: 'Proyecto comunitario' } });
    fireEvent.change(screen.getByLabelText('Descripción'), { target: { value: 'Proyecto de prueba' } });
    fireEvent.change(screen.getByLabelText('Área temática'), { target: { value: 'Tecnología' } });
    fireEvent.change(screen.getByLabelText('Unidad vecinal'), { target: { value: 'UV 1' } });
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2026-10-30' } });
    fireEvent.change(screen.getByLabelText('Término'), { target: { value: '2026-11-30' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar proyecto' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/proyectos', expect.objectContaining({
      method: 'POST', token: 'token-demo', body: expect.objectContaining({ title: 'Proyecto comunitario' })
    })));
    expect(await screen.findByText('Proyecto guardado.')).toBeTruthy();
  });
  it('impide guardar fechas invertidas', async () => {
    api.mockResolvedValue([]);
    render(React.createElement(ProyectosAdminPage));
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2026-11-30' } });
    fireEvent.change(screen.getByLabelText('Término'), { target: { value: '2026-10-30' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Guardar proyecto' }).closest('form')!);
    expect(await screen.findByText('La fecha de término debe ser posterior al inicio.')).toBeTruthy();
    expect(api.mock.calls.some(([path, options]) => path === '/api/proyectos' && options?.method === 'POST')).toBe(false);
  });
  it('edita y elimina un proyecto existente', async () => {
    const row = { id: 4, version: 0, title: 'Proyecto inicial', descripcion: 'Demo', area: 'Tecnología',
      unidadVecinal: 'UV 1', fechaInicio: '2026-10-30', fechaTermino: '2026-11-30', estado: 'activo' };
    api.mockImplementation(async (path, options) => path === '/api/proyectos' && !options?.method ? [row] : []);
    render(React.createElement(ProyectosAdminPage));
    expect(await screen.findByText('Proyecto inicial')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    fireEvent.change(screen.getByLabelText('Nombre del proyecto'), { target: { value: 'Proyecto corregido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar proyecto' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/proyectos/4', expect.objectContaining({ method: 'PUT', token: 'token-demo' })));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/proyectos/4', expect.objectContaining({ method: 'DELETE' })));
  });
});

describe('mantenedor de talleres', () => {
  it('envía geocerca con números al crear un taller', async () => {
    api.mockResolvedValue([]);
    const onRefresh = vi.fn(async () => {});
    render(React.createElement(TalleresAdminPage, { talleres: [], onRefresh }));
    const fields = [
      ['Nombre del taller', 'Taller prueba'], ['Descripción', 'Descripción demo'], ['Área temática', 'Tecnología'],
      ['Unidad vecinal', 'UV 1'], ['Comuna', 'La Florida'], ['Dirección', 'Sede demo'],
      ['Fecha y hora', '2026-10-30T10:00'], ['Coordinador', 'Monitor demo'], ['Cupos máximos', '20'],
      ['Latitud', '-33.45'], ['Longitud', '-70.67'], ['Radio en metros', '100']
    ];
    for (const [label, value] of fields) fireEvent.change(screen.getByLabelText(label), { target: { value } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos', expect.objectContaining({
      method: 'POST', token: 'token-demo', body: expect.objectContaining({ latitud: -33.45, longitud: -70.67, radioMetros: 100 })
    })));
    expect(onRefresh).toHaveBeenCalled();
  });
  it('edita y elimina un taller existente', async () => {
    const row = { id: 8, version: 0, title: 'Taller inicial', descripcion: 'Demo', area: 'Tecnología',
      unidadVecinal: 'UV 1', comuna: 'La Florida', direccion: 'Sede demo', fechaHora: '2026-10-30T10:00',
      docenteLider: 'Monitor demo', cuposMaximos: 20, proyectoId: null, latitud: null, longitud: null, radioMetros: null };
    api.mockResolvedValue([]);
    const onRefresh = vi.fn(async () => {});
    render(React.createElement(TalleresAdminPage, { talleres: [row], onRefresh }));
    fireEvent.click(screen.getByRole('button', { name: 'Editar' }));
    fireEvent.change(screen.getByLabelText('Nombre del taller'), { target: { value: 'Taller corregido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Guardar' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos/8', expect.objectContaining({ method: 'PUT' })));
    fireEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
    await waitFor(() => expect(api).toHaveBeenCalledWith('/api/eventos/8', expect.objectContaining({ method: 'DELETE' })));
  });
});
