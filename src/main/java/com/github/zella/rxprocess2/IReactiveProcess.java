package com.github.zella.rxprocess2;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;

public interface IReactiveProcess<T> {
    /**
     * Wait until process exits, Non zero exit code will be captured in {@link Exit}
     * <p>
     * Subscribe will start process execution
     *
     * @param timeout  timeout
     * @param timeUnit timeUnits
     * @return Cold Single
     */
    Single<Exit> waitDone(long timeout, TimeUnit timeUnit);

    /**
     * Wait until process exits with default timeout, Non-zero exit code will be captured in {@link Exit}.
     * <p>
     * Subscribe will start process execution
     * <p>
     * No timeout by default. Can be set via system property  {@code rxprocess2.timeOutMillis}
     *
     * @return Cold Single
     */
    Single<Exit> waitDone();

    /**
     * Process started callback
     *
     * @return Hot Single
     */
    Single<T> started();

    /**
     * Std input
     *
     * @return Subscriber allows to push stdin via {@code onNext(byte[] data)} and close stdin via {@code onComplete()}
     */
    Subscriber<byte[]> stdIn();

    /**
     * Real time process stdout/stderr callbacks.
     * <p>
     *
     * @return Hot Observable
     */
    Observable<ProcessChunk> stdOutErr();

    /**
     * Real time process stdout callbacks.
     *
     * @return Hot Observable
     */
    Observable<byte[]> stdOut();
}
