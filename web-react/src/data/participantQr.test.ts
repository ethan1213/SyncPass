import { describe, expect, it } from 'vitest';
import { participantCode, parseParticipantCode } from './participantQr';

const participant = '123e4567-e89b-12d3-a456-426614174000';
describe('QR de participante', () => {
  it('genera el mismo código que se lee con la cámara', () => {
    expect(parseParticipantCode(participantCode(participant))).toBe(`SP1:${participant}`);
  });
  it('acepta espacios alrededor de un código válido', () => {
    expect(parseParticipantCode(`  SP1:${participant}  `)).toBe(`SP1:${participant}`);
  });
  it.each(['', null, 'QR:otro', 'SP1:123', `SP1:${participant}extra`])('rechaza %s', value => {
    expect(() => parseParticipantCode(value)).toThrow('QR de participante inválido');
  });
});
