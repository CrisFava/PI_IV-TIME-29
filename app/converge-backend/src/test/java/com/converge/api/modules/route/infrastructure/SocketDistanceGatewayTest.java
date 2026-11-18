// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.api.modules.route.infrastructure;

import com.converge.api.modules.route.application.gateway.DistanceGateway.CoordinatePair;
import com.converge.api.modules.route.domain.model.subtypes.Coordinates;
import com.converge.socket.core.protocol.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SocketDistanceGatewayTest {

    // Porta sem ninguém ouvindo: força o caminho do handler local
    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static CoordinatePair pair(double lat1, double lon1, double lat2, double lon2) {
        return new CoordinatePair(new Coordinates(lat1, lon1), new Coordinates(lat2, lon2));
    }

    @Test
    @DisplayName("Sem servidor, calcula localmente o valor conhecido de 1 grau de longitude no equador")
    void shouldFallBackToLocalHandlerWhenServerIsDown() throws IOException {
        SocketDistanceGateway gateway = new SocketDistanceGateway("localhost", closedPort());

        double km = gateway.distanceKm(new Coordinates(0, 0), new Coordinates(0, 1));

        // 6371 * pi / 180, calculado à mão
        assertThat(km).isCloseTo(111.19492664455873, within(1e-9));
    }

    @Test
    @DisplayName("Sem servidor, o lote volta na mesma ordem dos pares")
    void shouldKeepBatchOrderOnFallback() throws IOException {
        SocketDistanceGateway gateway = new SocketDistanceGateway("localhost", closedPort());

        List<Double> distances = gateway.distancesKm(List.of(
                pair(0, 0, 0, 2),
                pair(0, 0, 0, 0),
                pair(0, 0, 0, 1)
        ));

        assertThat(distances).hasSize(3);
        assertThat(distances.get(0)).isCloseTo(2 * 111.19492664455873, within(1e-9));
        assertThat(distances.get(1)).isZero();
        assertThat(distances.get(2)).isCloseTo(111.19492664455873, within(1e-9));
    }

    @Test
    @DisplayName("Lista vazia devolve lista vazia sem procurar o servidor")
    void shouldReturnEmptyForEmptyList() throws IOException {
        SocketDistanceGateway gateway = new SocketDistanceGateway("localhost", closedPort());

        assertThat(gateway.distancesKm(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Lote maior que o limite do handler é dividido e mantém a ordem")
    void shouldSplitBatchLargerThanHandlerLimit() throws IOException {
        SocketDistanceGateway gateway = new SocketDistanceGateway("localhost", closedPort());
        List<CoordinatePair> pairs = new ArrayList<>();
        for (int i = 0; i < 2500; i++) {
            pairs.add(pair(0, 0, 0, (i % 90) / 10.0));
        }

        List<Double> distances = gateway.distancesKm(pairs);

        assertThat(distances).hasSize(2500);
        for (int i = 0; i < pairs.size(); i += 499) {
            assertThat(distances.get(i)).isCloseTo(111.19492664455873 * ((i % 90) / 10.0), within(1e-9));
        }
    }

    @Test
    @DisplayName("Servidor que responde outro tipo de mensagem faz o gateway usar o handler local")
    void shouldFallBackWhenServerAnswersAnotherMessageType() throws Exception {
        AtomicInteger connections = new AtomicInteger();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread thread = new Thread(() -> answerWithUnexpectedMessage(server, connections));
            thread.setDaemon(true);
            thread.start();
            SocketDistanceGateway gateway = new SocketDistanceGateway("localhost", server.getLocalPort());

            double km = gateway.distanceKm(new Coordinates(0, 0), new Coordinates(0, 1));

            assertThat(km).isCloseTo(111.19492664455873, within(1e-9));
            assertThat(connections.get()).isEqualTo(1);
        }
    }

    // Servidor falso: lê o pedido e responde com uma mensagem que não é DistanceResponse
    private static void answerWithUnexpectedMessage(ServerSocket server, AtomicInteger connections) {
        try (Socket client = server.accept()) {
            connections.incrementAndGet();
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            in.readObject();
            out.writeObject(new Message() {
            });
            out.flush();
        } catch (Exception ignored) {
            // O teste só confere o resultado do gateway
        }
    }
}
