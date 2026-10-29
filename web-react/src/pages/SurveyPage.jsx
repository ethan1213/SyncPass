import React, { useState } from 'react';

export default function SurveyPage({ talleres, onAddSurvey, isOfflineMode, onNavigate }) {
  const [selectedTallerId, setSelectedTallerId] = useState(1);
  const [rating, setRating] = useState(5);
  const [utilidad, setUtilidad] = useState('muy_util');
  const [comentario, setComentario] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    const surveyData = {
      id: Date.now(),
      tallerId: selectedTallerId,
      rating,
      utilidad,
      comentario: comentario || 'Sin comentarios adicionales',
      timestamp: new Date().toLocaleTimeString('es-CL'),
      synced: !isOfflineMode
    };

    onAddSurvey(surveyData);
    setSubmitted(true);
    setTimeout(() => {
      setSubmitted(false);
      setComentario('');
    }, 2500);
  };

  return (
    <div className="contenedor" style={{ padding: '30px 0 60px' }}>
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h1 style={{ fontSize: '26px', color: 'var(--dark)', marginBottom: '6px', textAlign: 'center' }}>
          Encuesta Rápida de Satisfacción (In Situ)
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '24px', textAlign: 'center' }}>
          Diseñada para aplicarse inmediatamente al término de la actividad comunitaria.
        </p>

        {submitted ? (
          <div className="alert alert-success" style={{ textAlign: 'center', padding: '30px' }}>
                        <h3>¡Evaluación Registrada con Éxito!</h3>
            <p style={{ fontSize: '14px', marginTop: '6px' }}>
              {isOfflineMode
                ? 'Guardada en la base de datos local (Room / SQLite) para sincronización posterior.'
                : 'Enviada directamente al servidor central de Vinculación con el Medio.'}
            </p>
          </div>
        ) : (
          <form
            onSubmit={handleSubmit}
            style={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: '10px',
              padding: '28px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.04)'
            }}
          >
            <div className="form-field">
              <label>Taller a Evaluar:</label>
              <select
                className="form-input"
                value={selectedTallerId}
                onChange={(e) => setSelectedTallerId(Number(e.target.value))}
              >
                {talleres.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.title} - {t.unidadVecinal}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-field" style={{ textAlign: 'center', margin: '24px 0' }}>
              <label style={{ fontSize: '16px', marginBottom: '10px' }}>
                1. ¿Cómo calificarías la sesión de hoy?
              </label>
              <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', marginTop: '8px' }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      fontSize: '36px',
                      cursor: 'pointer',
                      color: star <= rating ? '#f59e0b' : '#cbd5e1',
                      width: '48px',
                      height: '48px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'transform 0.15s ease'
                    }}
                  >
                    ★
                  </button>
                ))}
              </div>
              <span style={{ fontSize: '13px', color: 'var(--text-muted)', fontWeight: 'bold' }}>
                {rating === 5 ? 'Excelente (5 de 5)' : `${rating} de 5 estrellas`}
              </span>
            </div>

            <div className="form-field">
              <label>2. ¿Qué tan útil fue lo aprendido para tu vida cotidiana?</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px', marginTop: '8px' }}>
                {[
                  { key: 'muy_util', label: 'Muy Útil' },
                  { key: 'util', label: 'Útil' },
                  { key: 'regular', label: 'Regular' },
                  { key: 'poco_util', label: 'Poco Útil' }
                ].map((opt) => (
                  <button
                    key={opt.key}
                    type="button"
                    className={`btn btn-sm ${utilidad === opt.key ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ height: '42px' }}
                    onClick={() => setUtilidad(opt.key)}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="form-field" style={{ marginTop: '18px' }}>
              <label>3. Comentarios o sugerencias (opcional):</label>
              <textarea
                className="form-input"
                rows="3"
                value={comentario}
                onChange={(e) => setComentario(e.target.value)}
                placeholder="Escribe brevemente tu experiencia con los estudiantes monitores..."
              ></textarea>
            </div>

            <button type="submit" className="btn btn-primary btn-block" style={{ height: '48px', marginTop: '16px' }}>
              Guardar encuesta de impacto
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
