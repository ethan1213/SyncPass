import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import Badge from './Badge';

describe('badge de contador', () => {
  it('no renderiza nada cuando el contador es 0', () => {
    expect(renderToStaticMarkup(React.createElement(Badge, { count: 0 }))).toBe('');
  });
  it('no renderiza nada cuando el contador es negativo', () => {
    expect(renderToStaticMarkup(React.createElement(Badge, { count: -1 }))).toBe('');
  });
  it('muestra el número cuando hay pendientes', () => {
    const html = renderToStaticMarkup(React.createElement(Badge, { count: 3 }));
    expect(html).toContain('3');
    expect(html).toContain('badge-alerta');
  });
  it('permite className adicional y tono', () => {
    const html = renderToStaticMarkup(React.createElement(Badge, { count: 5, tone: 'alerta', className: 'extra' }));
    expect(html).toContain('extra');
  });
});
