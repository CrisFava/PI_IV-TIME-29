// Autor: Allan Giovanni Matias Paes - RA 25008211

package com.converge.api.shared.application;

public interface UseCase<T, R> {
    R execute(T request);
}
