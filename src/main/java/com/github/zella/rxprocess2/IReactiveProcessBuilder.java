package com.github.zella.rxprocess2;

import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.TimeUnit;

/**
 * Main class for process execution in reactive manner.
 * <p>
 * <p>
 * Notes:
 * <p>
 * By default cancellation of process give time to gracefully shutdown. Can be set via system property
 * <p>
 * {@code rxprocess2.gracefullStopSeconds}, default - "1". Disable - "-1"
 * <p>
 * Every procces has buffer, which holds last bytes from stderr, You will see it in {@link com.github.zella.rxprocess2.errors.ProcessException}.
 * Can be set via system property
 * <p>
 * {@code rxprocess2.stderrBuffer}, default - 4096
 */
public interface IReactiveProcessBuilder<T> {

    IReactiveProcessBuilder<T> withStdin(byte[] data);

    /**
     * Wait until process exits, Non zero exit code will be captured in {@link Exit}
     *
     * @param timeout  timeout
     * @param timeUnit timeUnits
     * @return Cold Single
     */
    Single<Exit> asWaitDone(long timeout, TimeUnit timeUnit);

    /**
     * Wait until process exits with default timeout, Non-zero exit code will be captured in {@link Exit}.
     * <p>
     * No timeout by default. Can be set via system property  {@code rxprocess2.timeOutMillis}
     *
     * @return Cold Single
     */
    Single<Exit> asWaitDone();

    /**
     * Wait process stdout. Non-zero exit code raise failure
     *
     * @return Cold Single
     */
    Single<byte[]> asStdOutSingle(long timeout, TimeUnit timeUnit);

    /**
     * Wait process stdout. Non-zero exit code raise failure
     * <p>
     * No timeout by default. Can be set via system property  {@code rxprocess2.timeOutMillis}
     *
     * @return Cold Single
     */
    Single<byte[]> asStdOutSingle();

    /**
     * Real time process stdout/stderr. Non-zero exit code raise failure
     *
     * @return Cold Observable
     */
    Observable<ProcessChunk> asStdErrOut(long timeout, TimeUnit timeUnit);

    /**
     * Real time process stdout/stderr. Non-zero exit code raise failure
     * <p>
     * No timeout by default. Can be set via system property  {@code rxprocess2.timeOutMillis}
     *
     * @return Cold Observable
     */
    Observable<ProcessChunk> asStdErrOut();

    /**
     * Real time process stdout. Non-zero exit code raise failure
     * <p>
     *
     * @return Cold Observable
     */
    Observable<byte[]> asStdOut(long timeout, TimeUnit timeUnit);

    /**
     * Real time process stdout. Non-zero exit code raise failure
     * <p>
     * No timeout by default. Can be set via system property  {@code rxprocess2.timeOutMillis}
     *
     * @return Cold Observable
     */
    Observable<byte[]> asStdOut();

    /**
     * Allow to push stdin to process. Use should not reuse this object after process ends
     *
     * @return Object used for stdin and stdout/stderr interaction
     */
    IReactiveProcess<T> biDirectional();
}
