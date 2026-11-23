// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.socket;

import com.converge.socket.core.client.ClientConnection;
import com.converge.socket.core.exception.ServerErrorCode;
import com.converge.socket.core.exception.ServerException;
import com.converge.socket.core.protocol.DisconnectRequest;
import com.converge.socket.core.protocol.Message;

import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Cliente do servidor de socket (a biblioteca com.converge.socket).
 *
 * Abre uma conexão TCP com o servidor, monta os streams de objetos
 * (ObjectOutputStream/ObjectInputStream) e entrega tudo para um ClientConnection,
 * que faz o envio e o recebimento das mensagens (Message). Esta classe só cuida
 * de conectar, enviar, receber e fechar.
 *
 * A conexão é aberta já no construtor: se o objeto foi criado, o servidor
 * respondeu. Por implementar AutoCloseable, pode ser usado em try-with-resources
 * (como faz o Setup), que fecha a conexão sozinho no final.
 */
@Getter
public class SocketClient implements AutoCloseable {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3000;

    private final String host;
    private final int port;
    private final Socket socket;
    private final ClientConnection connection;

    /**
     * Conecta no servidor padrão (localhost:3000), sem limite de tempo.
     */
    public SocketClient() throws ServerException {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }


    /**
     * Conecta em host e porta informados, sem limite de tempo (timeout 0).
     */
    public SocketClient(String host, int port) throws ServerException {
        this(host, port, 0);
    }
    // timeoutMillis > 0 limita a conexão e cada leitura; 0 mantém a espera sem limite.
    /**
     * Construtor principal. Usa o host e a porta padrão se vierem nulo ou
     * inválidos, conecta o socket e prepara os streams. A ordem importa: o
     * transmissor é criado e recebe flush primeiro, para enviar o cabeçalho que
     * o ObjectInputStream do servidor espera, e só depois o receptor é criado
     * (ele lê o cabeçalho do servidor). Se qualquer passo falhar, fecha o socket
     * para não deixá-lo aberto e lança ServerException (CONNECTION_ERROR) com o
     * host, a porta e o motivo.
     */
    public SocketClient(String host, int port, int timeoutMillis) throws ServerException {
        this.host = host != null ? host : DEFAULT_HOST;
        this.port = port > 0 ? port : DEFAULT_PORT;

        this.socket = new Socket();
        try {
            this.socket.connect(new InetSocketAddress(this.host, this.port), Math.max(timeoutMillis, 0));
            // Antes de criar os streams: o ObjectInputStream já lê o cabeçalho do servidor.
            this.socket.setSoTimeout(Math.max(timeoutMillis, 0));

            // Transmissor deve dar flush primeiro pra enviar o header
            ObjectOutputStream transmitter = new ObjectOutputStream(this.socket.getOutputStream());
            transmitter.flush();
            ObjectInputStream receiver = new ObjectInputStream(this.socket.getInputStream());

            this.connection = new ClientConnection(this.socket, receiver, transmitter);
        } catch (Exception e) {
            // se falhou no meio, fecha o socket pra ele não ficar aberto
            try {
                this.socket.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }
            throw new ServerException(
                    ServerErrorCode.CONNECTION_ERROR,
                    "Failed to connect to socket server at " + this.host + ":" + this.port + " - " + e.getMessage(),
                    e
            );
        }
    }

    // Envia uma requisição e espera uma resposta
    public Message send(Message request) throws ServerException {
        this.connection.send(request);
        return this.connection.receive();
    }

    // envia sem esperar uma resposta.
    public void sendOnly(Message message) throws ServerException {
        this.connection.send(message);
    }

    // equivalente a "envie" do servidor de continhas
    /**
     * Lê a próxima mensagem recebida do servidor, esperando se ainda não chegou
     * (respeitando o timeout, se houver).
     */
    public Message receive() throws ServerException {
        return this.connection.receive();
    }

    /**
     * Espia a próxima mensagem sem retirá-la da fila: uma chamada seguinte de
     * receive() ainda devolve a mesma mensagem.
     */
    public Message peek() throws ServerException {
        return this.connection.peek();
    }

    // equivalente a servidor.receba(new PedidoParaSair()) e servidor.adeus() no server de continhas
    /**
     * Encerra a conexão com educação: se ainda estiver aberta, avisa o servidor
     * com um DisconnectRequest ("Client closing") e, de qualquer jeito, fecha a
     * conexão. Qualquer erro é ignorado, então fechar nunca lança exceção.
     */

    @Override
    public void close() {
        try {
            if (!this.connection.isClosed()) {
                try {
                    this.connection.send(new DisconnectRequest("Client closing"));
                } finally{
                    this.connection.close();
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Diz se a conexão ainda está aberta do lado do cliente.
     */
    public boolean isConnected() {
        return !this.connection.isClosed();
    }

}
