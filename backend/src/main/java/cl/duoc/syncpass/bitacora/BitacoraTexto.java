package cl.duoc.syncpass.bitacora;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Procesamiento de texto de la bitacora, sin dependencias de Spring ni de la
 * base: se puede probar sola y por eso vive aparte del servicio.
 *
 * El resumen es extractivo (elige las frases mas representativas del propio
 * texto) y la respuesta a una pregunta siempre cita un fragmento real, nunca
 * inventa contenido.
 */
public final class BitacoraTexto {

    private static final Set<String> VACIAS = Set.of("para", "como", "donde", "cuando", "sobre", "este", "esta",
        "estos", "estas", "taller", "clase", "que", "con", "los", "las", "una", "uno", "del", "por", "porque",
        "cual", "cuales", "se", "de", "el", "la", "en", "un", "y", "a");

    private BitacoraTexto() {}

    /** Corta el texto en frases, que son la unidad que se cita al responder. */
    public static List<String> fragmentos(String contenido) {
        return Arrays.stream(contenido.split("(?<=[.!?])\\s+|\\R+"))
            .map(String::trim).filter(s -> !s.isBlank()).limit(100).toList();
    }

    /** Palabras con carga semantica: sin tildes, sin articulos y sin muletillas. */
    public static Set<String> terminos(String texto) {
        return Arrays.stream(Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").split("[^a-z0-9]+"))
            .filter(s -> s.length() > 2 && !VACIAS.contains(s))
            .collect(Collectors.toSet());
    }

    /** Las tres frases con mas terminos frecuentes, en el orden original. */
    public static String resumir(List<String> fragmentos) {
        var frecuencias = new HashMap<String, Integer>();
        fragmentos.forEach(parte -> terminos(parte).forEach(termino -> frecuencias.merge(termino, 1, Integer::sum)));
        return IntStream.range(0, fragmentos.size()).boxed()
            .sorted(Comparator.<Integer>comparingInt(i -> terminos(fragmentos.get(i)).stream()
                .mapToInt(t -> frecuencias.getOrDefault(t, 0)).sum()).reversed())
            .limit(3).sorted().map(fragmentos::get).collect(Collectors.joining(" "));
    }

    /** Indice del fragmento que mejor responde la pregunta, o -1 si ninguno coincide. */
    public static int mejorFragmento(List<String> fragmentos, String pregunta) {
        var buscados = terminos(pregunta);
        int mejor = -1, puntaje = 0;
        for (int i = 0; i < fragmentos.size(); i++) {
            var presentes = terminos(fragmentos.get(i));
            int actual = (int) buscados.stream().filter(presentes::contains).count();
            if (actual > puntaje) {
                puntaje = actual;
                mejor = i;
            }
        }
        return mejor;
    }
}
