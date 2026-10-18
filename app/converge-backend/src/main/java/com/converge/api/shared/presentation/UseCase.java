package com.converge.api.shared.presentation;

public interface UseCase<T, R> {
    R execute(T request);
}
