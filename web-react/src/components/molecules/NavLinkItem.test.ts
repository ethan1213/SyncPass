import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import NavLinkItem from './NavLinkItem';

vi.mock('wouter', () => ({
  Link: ({ children, href, className, 'aria-current': ariaCurrent }: { children: React.ReactNode; href: string; className?: string; 'aria-current'?: string }) =>
    React.createElement('a', { href, className, 'aria-current': ariaCurrent }, children),
}));

afterEach(() => cleanup());

it('marca el enlace activo y muestra el contador', () => {
  render(React.createElement(NavLinkItem, { href: '/sincronizacion', label: 'Sincronización', active: true, badgeCount: 4 }));
  const link = screen.getByRole('link', { name: /Sincronización/ });
  expect(link).toHaveProperty('href', expect.anything());
  expect(link.className).toContain('active');
  expect(screen.getByText('4')).toBeTruthy();
});

it('no muestra badge cuando no hay pendientes', () => {
  render(React.createElement(NavLinkItem, { href: '/', label: 'Inicio', active: false }));
  expect(screen.queryByText('0')).toBeNull();
});
