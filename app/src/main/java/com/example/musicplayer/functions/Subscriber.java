package com.example.musicplayer.functions;

/**
 * Created by haoping on 17/5/22.
 * TODO
 */
public interface Subscriber<T> {
    void onComplete(T t);
    void onError(Exception e);
}
