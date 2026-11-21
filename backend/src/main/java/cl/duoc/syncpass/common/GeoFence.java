package cl.duoc.syncpass.common;

/** Calcula la distancia superficial entre dos puntos WGS84 en metros. */
public final class GeoFence {

    private GeoFence() {}

    /** Veredicto de la geocerca, para que cada capa decida su propio codigo HTTP. */
    public enum Veredicto {
        ACEPTADA,
        /** El taller exige ubicacion y el cliente no la envio. */
        FALTA_UBICACION,
        /** Fuera del radio, o con un margen de error que no permite afirmarlo. */
        FUERA_DE_RADIO
    }

    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double a1 = Math.toRadians(lat1), a2 = Math.toRadians(lat2);
        double dLat = a2 - a1, dLon = Math.toRadians(lon2 - lon1);
        double h = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(a1) * Math.cos(a2) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371000.0 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    /**
     * Regla unica de acreditacion por ubicacion: la usan tanto el ingreso en
     * vivo como el lote que llega sincronizado desde el telefono, para que una
     * asistencia tomada sin señal se valide con el mismo criterio que una en linea.
     *
     * Un taller sin latitud no tiene geocerca y siempre acepta.
     */
    public static Veredicto evaluar(Double sitioLat, Double sitioLon, Double radioMetros,
                                    Double lat, Double lon, Float precisionMetros) {
        if (sitioLat == null || sitioLon == null || radioMetros == null) return Veredicto.ACEPTADA;
        if (lat == null || lon == null || precisionMetros == null) return Veredicto.FALTA_UBICACION;
        if (precisionMetros > 100) return Veredicto.FUERA_DE_RADIO;
        double distancia = distanceMeters(sitioLat, sitioLon, lat, lon);
        return distancia + precisionMetros > radioMetros ? Veredicto.FUERA_DE_RADIO : Veredicto.ACEPTADA;
    }
}
