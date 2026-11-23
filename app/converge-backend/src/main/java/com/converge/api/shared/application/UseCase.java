// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

// Contrato padrão dos use cases. Todos devem seguir essa estrutura
public interface UseCase<T, R> {
    R execute(T request);
}
