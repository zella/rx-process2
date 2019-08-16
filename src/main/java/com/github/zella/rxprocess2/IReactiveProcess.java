package com.github.zella.rxprocess2;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;

public interface IReactiveProcess<T> {
    Single<Exit> waitDone(long timeout, TimeUnit timeUnit);

    Single<Exit> waitDone();

    Single<T> started();

    Subscriber<byte[]> stdIn();

    Observable<ProcessChunk> stdOutErr();

    Observable<byte[]> stdOut();
}
