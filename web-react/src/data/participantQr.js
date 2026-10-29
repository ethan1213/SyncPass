export const participantCode = (sub) => `SP1:${sub}`;

export function parseParticipantCode(value) {
  const code = String(value || '').trim();
  if (!/^SP1:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(code)) throw new Error('QR de participante inválido. Usa un código SyncPass vigente.');
  return code;
}
