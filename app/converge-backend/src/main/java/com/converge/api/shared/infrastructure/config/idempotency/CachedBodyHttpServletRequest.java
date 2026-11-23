// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Wrapper de {@link HttpServletRequest} que armazena o corpo em memória respeitando um limite máximo em bytes.
 *
 * Existe porque o corpo de uma requisição HTTP só pode ser lido uma vez. O filtro
 * de idempotência precisa ler o corpo (para calcular a identidade da requisição)
 * e, depois, o controller também precisa lê-lo. Esta classe lê tudo uma vez, guarda
 * os bytes e permite que o corpo seja lido quantas vezes for preciso. Se o corpo
 * passar do limite, lança PayloadTooLargeException.
 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private static final int READ_BUFFER_SIZE = 4096;

    // Corpo completo da requisição, guardado em memória
    private final byte[] cachedBody;

    /**
     * Lê e guarda o corpo da requisição original, com o limite maxBodyBytes.
     * A leitura acontece já na construção, então o objeto nasce com o corpo pronto.
     * Lança PayloadTooLargeException se o corpo exceder o limite.
     */
    CachedBodyHttpServletRequest(HttpServletRequest request, int maxBodyBytes) throws IOException {
        super(request);
        this.cachedBody = readBoundedBody(request.getInputStream(), maxBodyBytes);
    }

    /**
     * Lê o corpo do stream em blocos de 4 KB e devolve os bytes. A cada bloco,
     * confere se o total acumulado passaria do limite e, se passar, aborta com
     * PayloadTooLargeException. Assim, um corpo gigante é cortado cedo, sem
     * carregar tudo na memória antes de rejeitar.
     */
    private static byte[] readBoundedBody(ServletInputStream inputStream, int maxBodyBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[READ_BUFFER_SIZE];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            if (totalRead + bytesRead > maxBodyBytes) {
                throw new PayloadTooLargeException();
            }
            buffer.write(chunk, 0, bytesRead);
            totalRead += bytesRead;
        }
        return buffer.toByteArray();
    }

    /**
     * Devolve os bytes do corpo guardados. Usado pelo filtro de idempotência
     * (mesmo pacote) para, por exemplo, calcular um hash do corpo.
     */
    byte[] getCachedBody() {
        return cachedBody;
    }

    /**
     * Devolve um novo ServletInputStream sobre os bytes guardados, para que o
     * corpo possa ser relido do começo a cada chamada. Funciona de forma
     * síncrona: está sempre pronto (isReady) e termina quando acabam os bytes.
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Leitura síncrona a partir do buffer em memória.
            }

            @Override
            public int read() {
                return inputStream.read();
            }
        };
    }


    /**
     * Devolve um leitor de texto (BufferedReader) sobre o corpo guardado,
     * decodificando em UTF-8.
     */
    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
