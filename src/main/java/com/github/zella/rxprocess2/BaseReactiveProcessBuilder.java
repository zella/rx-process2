package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.common.RxUtils;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

import static com.github.zella.rxprocess2.RxProcessConfig.DEFAULT_PROCESS_TIMEOUT_MILLIS;

public abstract class BaseReactiveProcessBuilder<T> implements IReactiveProcessBuilder<T> {

    protected byte[] stdin = {};

    @Override
    public IReactiveProcessBuilder<T> withStdin(byte[] data) {
        this.stdin = data;
        return this;
    }

    @Override
    public Single<Exit> asWaitDone() {
        return asWaitDone(DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public Single<byte[]> asStdOutSingle(long timeout, TimeUnit timeUnit) {
        return RxUtils.collect(asStdErrOut(timeout, timeUnit)
                .filter(c -> !c.isStdErr)
                .map(c -> c.data));
    }

    @Override
    public Single<byte[]> asStdOutSingle() {
        return asStdOutSingle(DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }


    @Override
    public Observable<ProcessChunk> asStdErrOut() {
        return asStdErrOut(DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public Observable<byte[]> asStdOut(long timeout, TimeUnit timeUnit) {
        return asStdErrOut(timeout, timeUnit).filter(c -> !c.isStdErr).map(c -> c.data);
    }

    @Override
    public Observable<byte[]> asStdOut() {
        return asStdOut(DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }


}
