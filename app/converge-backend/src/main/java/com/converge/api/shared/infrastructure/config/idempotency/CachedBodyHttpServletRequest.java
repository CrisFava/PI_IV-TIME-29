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
 */
final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private static final int READ_BUFFER_SIZE = 4096;

    private final byte[] cachedBody;

    CachedBodyHttpServletRequest(HttpServletRequest request, int maxBodyBytes) throws IOException {
        super(request);
        this.cachedBody = readBoundedBody(request.getInputStream(), maxBodyBytes);
    }

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

    byte[] getCachedBody() {
        return cachedBody;
    }

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

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
