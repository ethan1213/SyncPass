import React from 'react';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import Button from './Button';

afterEach(() => cleanup());

it('usa btn-primary por defecto y ejecuta onClick', () => {
  const onClick = vi.fn();
  render(React.createElement(Button, { onClick }, 'Guardar'));
  const btn = screen.getByRole('button', { name: 'Guardar' });
  expect(btn.className).toContain('btn-primary');
  fireEvent.click(btn);
  expect(onClick).toHaveBeenCalled();
});

it('aplica variante, block y small', () => {
  render(React.createElement(Button, { variant: 'secondary', block: true, small: true }, 'Cancelar'));
  const btn = screen.getByRole('button', { name: 'Cancelar' });
  expect(btn.className).toContain('btn-secondary');
  expect(btn.className).toContain('btn-block');
  expect(btn.className).toContain('btn-sm');
});
