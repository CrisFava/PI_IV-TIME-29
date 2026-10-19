package com.converge.api.shared.application;

public interface UseCase<T, R> {
    R execute(T request);
}
