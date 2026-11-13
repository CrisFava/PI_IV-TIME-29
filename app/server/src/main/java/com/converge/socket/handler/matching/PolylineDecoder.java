// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import java.util.ArrayList;
import java.util.List;

/**
 * Lê o caminho codificado no formato "Encoded Polyline" do Google (o mesmo que a
 * Routes API devolve) e devolve os pontos como pares {latitude, longitude}.
 */
public final class PolylineDecoder {

    private PolylineDecoder() {
    }

    public static List<double[]> decode(String encoded) {
        List<double[]> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return points;
        }

        int index = 0;
        int lat = 0;
        int lng = 0;
        try {
            while (index < encoded.length()) {
                int[] latStep = nextValue(encoded, index);
                lat += latStep[0];
                int[] lngStep = nextValue(encoded, latStep[1]);
                lng += lngStep[0];
                index = lngStep[1];
                points.add(new double[]{lat / 1e5, lng / 1e5});
            }
        } catch (StringIndexOutOfBoundsException e) {
            // Polyline corrompida: quem chamou cai na linha reta
            return new ArrayList<>();
        }
        return points;
    }

    // Devolve {valor, próxima posição}
    private static int[] nextValue(String encoded, int start) {
        int result = 0;
        int shift = 0;
        int index = start;
        int chunk;
        do {
            chunk = encoded.charAt(index++) - 63;
            result |= (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20);

        int value = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new int[]{value, index};
    }
}
