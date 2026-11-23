// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.distance;

import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários para {@link DistanceHandler}.
 * Valida o cálculo em lote de distâncias geodésicas utilizando a fórmula de Haversine entre múltiplos pontos geográficos.
 * <p>
 * Os métodos de teste estão bem explicativos através de suas anotações {@code @DisplayName}.
 */
class DistanceHandlerTest {

    // Um grau de arco no raio de 6371 km: 6371 * pi / 180.
    private static final double ONE_DEGREE_KM = 111.19492664455873;

    private final DistanceHandler handler = new DistanceHandler();

    private double distance(double lat1, double lng1, double lat2, double lng2) throws ServerException {
        return handler.execute(new DistanceRequest(lat1, lng1, lat2, lng2)).getDistancesKm()[0];
    }

    private double[] batch(double[]... pairs) throws ServerException {
        return handler.execute(new DistanceRequest(pairs)).getDistancesKm();
    }

    private void assertInvalidMessage(double[][] pairs) {
        ServerException error = assertThrows(ServerException.class, () -> handler.execute(new DistanceRequest(pairs)));
        assertEquals(ServerErrorCode.INVALID_MESSAGE, error.getErrorCode());
    }

    @Test
    @DisplayName("Pontos iguais devem ter distância zero")
    void shouldReturnZeroForSamePoint() throws ServerException {
        assertEquals(0.0, distance(-22.9056, -47.0608, -22.9056, -47.0608), 0.0);
    }

    @Test
    @DisplayName("Um grau de latitude deve dar 6371 * pi / 180 km")
    void shouldMeasureOneDegreeOfLatitude() throws ServerException {
        assertEquals(ONE_DEGREE_KM, distance(0, 0, 1, 0), 1e-9);
    }

    @Test
    @DisplayName("Um grau de longitude no equador deve dar o mesmo valor de um grau de latitude")
    void shouldMeasureOneDegreeOfLongitudeAtEquator() throws ServerException {
        assertEquals(ONE_DEGREE_KM, distance(0, 10, 0, 11), 1e-9);
    }

    @Test
    @DisplayName("Do equador ao polo deve dar um quarto da circunferência")
    void shouldMeasureEquatorToPole() throws ServerException {
        assertEquals(ONE_DEGREE_KM * 90, distance(0, 0, 90, 0), 1e-6);
    }

    @Test
    @DisplayName("Pontos opostos no equador devem dar meia circunferência")
    void shouldMeasureAntipodalPoints() throws ServerException {
        assertEquals(ONE_DEGREE_KM * 180, distance(0, 0, 0, 180), 1e-6);
    }

    @Test
    @DisplayName("A distância deve ser a mesma nos dois sentidos")
    void shouldBeSymmetric() throws ServerException {
        double ida = distance(-22.9056, -47.0608, -23.5505, -46.6333);
        double volta = distance(-23.5505, -46.6333, -22.9056, -47.0608);
        assertEquals(ida, volta, 1e-9);
    }

    @Test
    @DisplayName("Campinas a São Paulo deve ficar perto de 85 km em linha reta")
    void shouldMeasureCampinasToSaoPaulo() throws ServerException {
        double km = distance(-22.9056, -47.0608, -23.5505, -46.6333);
        assertTrue(km > 80 && km < 90, "distância fora do esperado: " + km);
    }

    @Test
    @DisplayName("Lote deve devolver as distâncias na mesma ordem dos pares")
    void shouldKeepBatchOrder() throws ServerException {
        double[] result = batch(
                new double[]{0, 0, 1, 0},
                new double[]{0, 0, 0, 0},
                new double[]{0, 0, 90, 0});
        assertEquals(3, result.length);
        assertEquals(ONE_DEGREE_KM, result[0], 1e-9);
        assertEquals(0.0, result[1], 0.0);
        assertEquals(ONE_DEGREE_KM * 90, result[2], 1e-6);
    }

    @Test
    @DisplayName("Cada par do lote deve dar o mesmo valor do cálculo de um par isolado")
    void shouldMatchSinglePairInBatch() throws ServerException {
        double[] a = {-22.9056, -47.0608, -23.5505, -46.6333};
        double[] b = {-22.8, -47.2, -22.95, -47.05};
        double[] result = batch(a, b);
        assertEquals(distance(a[0], a[1], a[2], a[3]), result[0], 0.0);
        assertEquals(distance(b[0], b[1], b[2], b[3]), result[1], 0.0);
        assertNotEquals(result[0], result[1]);
    }

    @Test
    @DisplayName("Lista vazia deve devolver resposta vazia")
    void shouldReturnEmptyForEmptyList() throws ServerException {
        assertArrayEquals(new double[0], batch());
    }

    @Test
    @DisplayName("Lote com o máximo de pares deve ser aceito")
    void shouldAcceptMaxBatch() throws ServerException {
        double[][] pairs = new double[DistanceHandler.MAX_PAIRS][];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new double[]{0, 0, 1, 0};
        }
        assertEquals(DistanceHandler.MAX_PAIRS, batch(pairs).length);
    }

    @Test
    @DisplayName("Lote acima do máximo deve ser rejeitado")
    void shouldRejectBatchAboveMax() {
        double[][] pairs = new double[DistanceHandler.MAX_PAIRS + 1][];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new double[]{0, 0, 1, 0};
        }
        assertInvalidMessage(pairs);
    }

    @Test
    @DisplayName("Request nulo deve ser rejeitado")
    void shouldRejectNullRequest() {
        ServerException error = assertThrows(ServerException.class, () -> handler.execute(null));
        assertEquals(ServerErrorCode.INVALID_MESSAGE, error.getErrorCode());
    }

    @Test
    @DisplayName("Lista de pares nula deve ser rejeitada")
    void shouldRejectNullList() {
        assertInvalidMessage(null);
    }

    @Test
    @DisplayName("Par nulo ou com tamanho diferente de 4 deve ser rejeitado")
    void shouldRejectMalformedPair() {
        assertInvalidMessage(new double[][]{null});
        assertInvalidMessage(new double[][]{{0, 0, 1}});
        assertInvalidMessage(new double[][]{{0, 0, 1, 0, 5}});
    }

    @Test
    @DisplayName("Latitude fora de -90 a 90 deve ser rejeitada na origem e no destino")
    void shouldRejectLatitudeOutOfRange() {
        assertInvalidMessage(new double[][]{{90.0001, 0, 0, 0}});
        assertInvalidMessage(new double[][]{{-90.0001, 0, 0, 0}});
        assertInvalidMessage(new double[][]{{0, 0, 90.0001, 0}});
        assertInvalidMessage(new double[][]{{0, 0, -90.0001, 0}});
    }

    @Test
    @DisplayName("Longitude fora de -180 a 180 deve ser rejeitada na origem e no destino")
    void shouldRejectLongitudeOutOfRange() {
        assertInvalidMessage(new double[][]{{0, 180.0001, 0, 0}});
        assertInvalidMessage(new double[][]{{0, -180.0001, 0, 0}});
        assertInvalidMessage(new double[][]{{0, 0, 0, 180.0001}});
        assertInvalidMessage(new double[][]{{0, 0, 0, -180.0001}});
    }

    @Test
    @DisplayName("Valores nos limites exatos devem ser aceitos")
    void shouldAcceptBoundaryValues() throws ServerException {
        assertEquals(ONE_DEGREE_KM * 180, distance(90, 180, -90, -180), 1e-6);
    }

    @Test
    @DisplayName("NaN e infinito devem ser rejeitados")
    void shouldRejectNanAndInfinity() {
        assertInvalidMessage(new double[][]{{Double.NaN, 0, 0, 0}});
        assertInvalidMessage(new double[][]{{0, Double.NaN, 0, 0}});
        assertInvalidMessage(new double[][]{{0, 0, Double.POSITIVE_INFINITY, 0}});
        assertInvalidMessage(new double[][]{{0, 0, 0, Double.NEGATIVE_INFINITY}});
    }

    @Test
    @DisplayName("Mensagem de erro deve indicar o par inválido")
    void shouldReportInvalidPairIndex() {
        ServerException error = assertThrows(ServerException.class, () -> handler.execute(
                new DistanceRequest(new double[][]{{0, 0, 1, 0}, {0, 0, 95, 0}})));
        assertTrue(error.getMessage().contains("par 1"), error.getMessage());
        assertTrue(error.getMessage().contains("Latitude"), error.getMessage());
    }
}
