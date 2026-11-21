import React from 'react';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import MyQrPage from './MyQrPage';

const useSessionMock = vi.fn();
vi.mock('../auth/SessionProvider', () => ({ useSession: () => useSessionMock() }));

beforeEach(() => useSessionMock.mockReset());
afterEach(() => cleanup());

it('genera el QR a partir del sub del usuario', async () => {
  useSessionMock.mockReturnValue({ user: { sub: 'abc-123' } });
  render(React.createElement(MyQrPage));
  expect(screen.getByText('SP1:abc-123')).toBeTruthy();
  await waitFor(() => expect(screen.getByAltText('QR personal de acreditación SyncPass')).toHaveProperty('src', expect.stringContaining('data:image')));
});

it('no genera código cuando no hay usuario', () => {
  useSessionMock.mockReturnValue({ user: null });
  render(React.createElement(MyQrPage));
  expect(screen.queryByAltText('QR personal de acreditación SyncPass')).toBeNull();
});
