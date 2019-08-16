package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.common.CircularFifoQueue;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.ReplayProcessor;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Subscriber;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.github.zella.rxprocess2.RxProcessConfig.DEFAULT_PROCESS_TIMEOUT_MILLIS;
import static com.github.zella.rxprocess2.RxProcessConfig.STDERR_BUFF_SIZE;

public abstract class BaseReactiveProcess<T> implements IReactiveProcess<T> {

    protected final AsyncSubject<T> startedSubject = AsyncSubject.create();

    protected final PublishSubject<ProcessChunk> stdoutStdErrSubject = PublishSubject.create();

    protected final Collection<Byte> stderrBuffer = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

    protected final ReplayProcessor<byte[]> stdinProcessor = ReplayProcessor.create();

    @Override
    public Single<Exit> waitDone() {
        return waitDone(DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }


    @Override
    public Single<T> started() {
        return startedSubject.singleOrError();
    }

    @Override
    public Subscriber<byte[]> stdIn() {
        return stdinProcessor;
    }

    @Override
    public Observable<ProcessChunk> stdOutErr() {
        return stdoutStdErrSubject;
    }

    @Override
    public Observable<byte[]> stdOut() {
        return stdoutStdErrSubject.filter(c -> !c.isStdErr).map(c -> c.data);
    }
}
