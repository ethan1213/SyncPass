import React from 'react';
import { Link } from 'wouter';

/**
 * Detalle de un taller / evento en terreno.
 * Cubre la informacion que el caso §3.2 exige por evento: proyecto asociado, nombre de
 * la actividad, fecha/hora programada, cupos maximos, recinto/direccion, coordenadas
 * referenciales, y el registro de asistencia con su estado de sincronizacion.
 */
export default function TallerDetailPage({ taller }) {
  if (!taller) {
    return (
      <div className="contenedor estado-bloque">
        <h2>No encontramos ese taller</h2>
        <p className="texto-apoyo">
          Puede que el evento haya cerrado o que el enlace esté mal escrito.
        </p>
        <Link href="/" className="btn btn-primary">
          Volver al panel de talleres
        </Link>
      </div>
    );
  }

  const sincronizados = (taller.asistentes || []).filter((a) => a.synced).length;
  const pendientes = (taller.asistentes || []).length - sincronizados;

  return (
    <main className="contenedor" style={{ padding: '24px 0 60px' }}>
      <Link href="/" className="enlace-volver">
        ← Volver al panel de talleres
      </Link>

      <div className="detail-layout">
        <div className="detail-img-wrap">
          <img src={taller.image} alt={`Taller: ${taller.title}`} />
        </div>

        <div>
          <span className="tarjeta-cat">
            {taller.area} · {taller.comuna}
          </span>
          <h1 className="titulo-detalle">{taller.title}</h1>
          <p className="texto-apoyo">{taller.descripcion}</p>

          <div className="quorum-bloque">
            <div className="quorum-encabezado">
              <span>
                Quórum inscrito: <strong>{taller.inscritos} / {taller.cuposMaximos}</strong>
              </span>
              <strong className="quorum-pct">{taller.quorumPct}%</strong>
            </div>
            <div
              className="barra-quorum"
              role="progressbar"
              aria-valuenow={taller.quorumPct}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`Quórum del taller ${taller.title}`}
            >
              <div className="barra-quorum-relleno" style={{ width: `${taller.quorumPct}%` }} />
            </div>
          </div>

          <dl className="ficha-datos">
            <div>
              <dt>Fecha y hora programada</dt>
              <dd>{taller.fechaHora}</dd>
            </div>
            <div>
              <dt>Unidad vecinal responsable</dt>
              <dd>{taller.unidadVecinal}</dd>
            </div>
            <div>
              <dt>Recinto / dirección</dt>
              <dd>
                {taller.direccion}, {taller.comuna}
              </dd>
            </div>
            <div>
              <dt>Docente líder / coordinador(a)</dt>
              <dd>{taller.docenteLider}</dd>
            </div>
            <div>
              <dt>Área temática</dt>
              <dd>{taller.area}</dd>
            </div>
            <div>
              <dt>Cupos máximos</dt>
              <dd>{taller.cuposMaximos}</dd>
            </div>
          </dl>

          <div className="acciones-fila">
            <Link href="/acreditar" className="btn btn-primary">
              Acreditar asistencia por QR
            </Link>
            <Link href="/encuesta" className="btn btn-secondary">
              Aplicar encuesta en terreno
            </Link>
          </div>
        </div>
      </div>

      <section className="seccion-bloque">
        <div className="seccion-encabezado">
          <h2>Registro de asistencia</h2>
          <p className="texto-apoyo">
            {sincronizados} sincronizado(s) con la API · {pendientes} pendiente(s) de envío
            (<code>is_synced = 0</code>)
          </p>
        </div>

        {(taller.asistentes || []).length === 0 ? (
          <p className="alert alert-info">
            Todavía no hay marcaciones registradas para este taller.
          </p>
        ) : (
          <div className="tabla-scroll">
            <table className="tabla-datos">
              <caption className="visualmente-oculto">
                Marcaciones de asistencia del taller {taller.title}
              </caption>
              <thead>
                <tr>
                  <th scope="col">Identificador</th>
                  <th scope="col">Participante</th>
                  <th scope="col">Hora de marcación</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Sincronización</th>
                </tr>
              </thead>
              <tbody>
                {taller.asistentes.map((a) => (
                  <tr key={a.id}>
                    <td>
                      <code>{a.id}</code>
                    </td>
                    <td>{a.nombre}</td>
                    <td>{a.hora}</td>
                    <td>{a.estado}</td>
                    <td>
                      <span className={`pill ${a.synced ? 'pill-ok' : 'pill-pendiente'}`}>
                        {a.synced ? 'Sincronizado' : 'Pendiente'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
