package com.example.musicplayer.functions;

/**
 * Created by haoping on 17/5/23.
 */
public interface Function<T, R> {
    R apply(T t);
}
