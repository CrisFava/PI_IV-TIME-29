// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.infrastructure.config.idempotency;

import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


/**
 * Calcula o fingerprint SHA-256 de uma requisição HTTP para detectar reutilização indevida de chave de idempotência.
 *
 * O que é o fingerprint: uma "impressão digital" da requisição, um texto
 * hexadecimal de 64 caracteres. Requisições idênticas geram sempre o mesmo
 * valor; qualquer diferença no método, no caminho, na query ou no corpo gera
 * um valor completamente diferente.
 *
 * Para que serve: o IdempotencyFilter guarda o fingerprint junto com a chave.
 * Quando a mesma Idempotency-Key chega de novo, ele compara os fingerprints:
 * - iguais: é uma retentativa legítima, então devolve a resposta guardada;
 * - diferentes: alguém reutilizou a chave em outra requisição (ex.: mesma chave
 *   para aceitar dois passageiros diferentes), então responde BAD_REQUEST.
 *
 * Por que hash e não o corpo inteiro: guarda só 64 caracteres por chave em vez
 * de uma cópia do corpo, e a comparação é uma simples igualdade de texto.
 *
 * Classe utilitária: só tem método estático e não guarda estado.
 */
@NoArgsConstructor
final class RequestFingerprint {

    /**
     * Gera o fingerprint a partir dos quatro elementos que identificam a requisição:
     *
     * @param method      método HTTP já em maiúsculas (POST, PUT ou PATCH)
     * @param path        caminho da requisição (ex.: /api/v1/rides/abc/cancel)
     * @param queryString query string crua, ou null se não houver
     * @param body        bytes do corpo guardados pelo CachedBodyHttpServletRequest
     * @return SHA-256 em hexadecimal minúsculo (64 caracteres)
     * @throws IllegalStateException se o SHA-256 não existir na JVM (na prática
     *                               nunca acontece, pois todo Java é obrigado a ter)
     */
    static String compute(String method, String path, String queryString, byte[] body) {



        // 4. Corpo, nos bytes exatos em que chegou
        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 1. Método, seguido de '\n' como separador
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');

            // 2. Caminho, seguido de '\n'
            digest.update(path.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            // 3. Query string (pulada se for null), seguida de '\n'
            //    Os separadores evitam ambiguidade: sem eles, "AB" + "C" e
            //    "A" + "BC" gerariam o mesmo hash.
            if (queryString != null) {
                digest.update(queryString.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) '\n');
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
    }
}
