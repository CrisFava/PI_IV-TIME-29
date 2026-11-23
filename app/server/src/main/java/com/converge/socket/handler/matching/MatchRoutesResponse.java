// Autor: Gustavo Alves de Siqueira Costa - RA 25001650

package com.converge.socket.handler.matching;

import com.converge.socket.core.protocol.Message;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Combinações compatíveis, já ordenadas da mais para a menos compatível.
 */
public class MatchRoutesResponse extends Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<MatchResult> results;

    /**
     * Cria a resposta com as combinações compatíveis encontradas.
     *
     * @param results lista de resultados compatíveis já ordenados por relevância
     */
    public MatchRoutesResponse(List<MatchResult> results) {
        this.results = results == null ? new ArrayList<>() : new ArrayList<>(results);
    }

    // Getters e setters
    public List<MatchResult> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "MatchRoutesResponse{results=" + results.size() + "}";
    }
}
