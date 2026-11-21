/** `qrcode` no publica tipos propios ni hay un paquete @types/qrcode; se tipa el
 * único método que usa SyncPass (generar un PNG como data URL). */
declare module 'qrcode' {
  interface QRCodeToDataURLOptions {
    width?: number;
    margin?: number;
    errorCorrectionLevel?: 'L' | 'M' | 'Q' | 'H';
  }

  function toDataURL(text: string, options?: QRCodeToDataURLOptions): Promise<string>;

  const QRCode: { toDataURL: typeof toDataURL };
  export default QRCode;
}
