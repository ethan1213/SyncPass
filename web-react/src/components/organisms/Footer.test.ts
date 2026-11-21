import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import Footer from './Footer';

describe('pie de página', () => {
  it('muestra la marca y la autoría', () => {
    const html = renderToStaticMarkup(React.createElement(Footer));
    expect(html).toContain('SyncPass');
    expect(html).toContain('Ethan Astorga');
  });
});
