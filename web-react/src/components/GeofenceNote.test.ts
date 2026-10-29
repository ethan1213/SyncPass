import { describe, expect, it } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import GeofenceNote from './GeofenceNote';

describe('aviso de geocerca', () => {
  it('explica cuándo se necesita GPS', () => {
    expect(renderToStaticMarkup(React.createElement(GeofenceNote, { required: true }))).toContain('ubicación GPS');
  });
  it('no muestra aviso en talleres sin geocerca', () => {
    expect(renderToStaticMarkup(React.createElement(GeofenceNote, { required: false }))).toBe('');
  });
});
