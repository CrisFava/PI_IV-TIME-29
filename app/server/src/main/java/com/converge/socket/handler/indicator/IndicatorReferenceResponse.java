// Autor: Daniela Mikie Kikuchi Gonçalves - RA 25003068

package com.converge.socket.handler.indicator;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;

/** Referências na mesma ordem do pedido: "2025-W20", "2025-05", "2025-S1" ou "ALL_TIME". */
public class IndicatorReferenceResponse extends Message {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String[] references;

    public IndicatorReferenceResponse(String[] references) {
        this.references = references;
    }

    public String[] getReferences() {
        return references;
    }

    @Override
    public String toString() {
        return "IndicatorReferenceResponse{size=" + (references == null ? 0 : references.length) + "}";
    }
}