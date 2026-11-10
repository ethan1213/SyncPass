import React, { useState } from 'react';
import { Link } from 'wouter';

export default function HomePage({ talleres, onRefresh, onSelectTaller }) {
  const [refreshing, setRefreshing] = useState(false);
  const [refreshError, setRefreshError] = useState('');
  async function refreshTalleres() {
    setRefreshing(true);
    setRefreshError('');
    try { await onRefresh(); }
    catch { setRefreshError('No se pudo actualizar. Inténtalo de nuevo cuando vuelva la conexión.'); }
    finally { setRefreshing(false); }
  }
  return (
    <>
      <main className="contenedor">
        <section className="bienvenida" aria-labelledby="titulo-inicio">
          <p className="eyebrow">CITT Plaza Vespucio · Vinculación con el medio</p>
          <h1 id="titulo-inicio">En terreno.<br />Sin papel.</h1>
          <p>Organiza actividades comunitarias, acredita asistentes y recoge su opinión desde la web o el teléfono. Si se pierde la señal, los registros pendientes quedan guardados para enviarlos después.</p>
          <div className="inicio-acciones">
            <a className="btn btn-primary" href="#talleres">Explorar talleres</a>
            <Link className="btn btn-secondary" href="/mi-panel">Ir a mi panel</Link>
          </div>
          <div className="inicio-enlaces">
            <a href="/descargas/SyncPass-Android.apk" download="SyncPass-Android.apk">Descargar app Android de prueba</a>
            <a href="/mockups.html">Ver mockups y modelo de datos</a>
          </div>
          <p className="apk-ayuda">En Android, abre el archivo descargado desde Descargas para instalar la app.</p>
        </section>

        <section className="beneficios" aria-labelledby="titulo-beneficios">
          <h2 id="titulo-beneficios">¿Por qué SyncPass?</h2>
          <div className="grid-beneficios">
            <div className="beneficio-card">
              <h3>Acreditación en segundos</h3>
              <p>
                El ingreso se registra leyendo un código QR, sin filas ni listas
                escritas a mano en la entrada.
              </p>
            </div>
            <div className="beneficio-card">
              <h3>Encuestas en el momento</h3>
              <p>
                La valoración del taller se responde ahí mismo al terminar, no
                semanas después por correo.
              </p>
            </div>
            <div className="beneficio-card">
              <h3>Funciona sin señal</h3>
              <p>
                En unidades vecinales sin cobertura, los registros quedan en el
                dispositivo y se envían al recuperar la conexión.
              </p>
            </div>
          </div>
        </section>

        <section id="talleres" aria-labelledby="titulo-talleres">
          <div className="catalogo-cabecera">
            <div>
              <h2 id="titulo-talleres">Talleres en terreno</h2>
              <p>Actividades comunitarias en curso y su quórum de inscripción.</p>
            </div>
            <button className="btn btn-secondary" type="button" onClick={refreshTalleres} disabled={refreshing}>
              {refreshing ? 'Actualizando…' : 'Actualizar talleres'}
            </button>
          </div>

          {refreshError && <p className="alert alert-warning" role="status">{refreshError}</p>}

          {!talleres.length && <div className="panel inicio-vacio"><h3>Aún no hay talleres publicados</h3><p>Vuelve más tarde para revisar las próximas actividades comunitarias.</p></div>}
          <div className="proyectos-grid">
            {talleres.map((taller) => (
              <article key={taller.id} className="tarjeta-proyecto">
                <div className="tarjeta-img-wrap">
                  <img src={taller.image} alt={`Taller de ${taller.title}`} />
                </div>
                <div className="tarjeta-body">
                  <span className="tarjeta-cat">
                    {taller.area} · {taller.comuna}
                  </span>
                  <h3>{taller.title}</h3>
                  <p className="tarjeta-desc">{taller.descripcion}</p>

                  <div className="quorum-bloque">
                    <div className="quorum-encabezado">
                      <span>
                        Quórum{' '}
                        <strong>
                          {taller.inscritos} / {taller.cuposMaximos}
                        </strong>
                      </span>
                      <strong className="quorum-pct">{taller.quorumPct}%</strong>
                    </div>
                    <div
                      className="barra-quorum"
                      role="progressbar"
                      aria-valuenow={taller.quorumPct}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-label={`Quórum de ${taller.title}`}
                    >
                      <div
                        className="barra-quorum-relleno"
                        style={{ width: `${taller.quorumPct}%` }}
                      />
                    </div>
                  </div>

                  <div className="tarjeta-footer">
                    <span className="tarjeta-lugar">{taller.unidadVecinal}</span>
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={() => onSelectTaller(taller)}
                    >
                      Ver taller
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </section>

      </main>
    </>
  );
}
