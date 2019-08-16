package com.github.zella.rxprocess2;

import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

public interface IReactiveProcessBuilder<T> {

    Single<Exit> asWaitDone(long timeout, TimeUnit timeUnit);

    Single<Exit> asWaitDone();

    Single<byte[]> asStdOutSingle(long timeout, TimeUnit timeUnit);

    Single<byte[]> asStdOutSingle();

    Observable<ProcessChunk> asStdErrOut(long timeout, TimeUnit timeUnit);

    Observable<ProcessChunk> asStdErrOut();

    Observable<byte[]> asStdOut(long timeout, TimeUnit timeUnit);

    Observable<byte[]> asStdOut();

    IReactiveProcess<T> biDirectional();
}
