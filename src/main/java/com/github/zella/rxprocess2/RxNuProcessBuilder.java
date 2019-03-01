package com.github.zella.rxprocess2;

import com.zaxxer.nuprocess.NuProcessBuilder;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Builder for constructing process
 *
 * @author zella
 */
public class RxNuProcessBuilder {

    NuProcessBuilder builder;

    private RxNuProcessBuilder(List<String> command) {
        builder = new NuProcessBuilder(command);
    }

    /**
     * Set environment variables
     *
     * @param environment vars
     * @return builder
     */
    public RxNuProcessBuilder withEnv(Map<String, String> environment) {
        builder = new NuProcessBuilder(builder.command(), environment);
        return this;
    }

    /**
     * Set working directory
     *
     * @param cwd workdir
     * @return
     */
    public RxNuProcessBuilder withCwd(Path cwd) {
        builder.setCwd(cwd);
        return this;
    }

    /**
     * Represent process as single cold stream, where, onCompleted signals correct process exit, onError - exit with non zero code,
     * timeout or any others errors. Canceling force kills process. Note, that base class for error @see {@link com.github.zella.rxprocess2.errors.ProcessException}
     *
     * @param timeout  process timeout
     * @param timeUnit process timeout units
     * @return stdout observable
     */
    public Observable<byte[]> asStdOut(long timeout, TimeUnit timeUnit) {
        return new AsStdOut(this).create(timeout, timeUnit);
    }

    /**
     * Same as asStdOut, with default timeout. Default timeout - 60 sec, can be set via rxprocess2.defaultTimeoutMillis property
     *
     * @return stdout observable
     */
    public Observable<byte[]> asStdOut() {
        return new AsStdOut(this).create(BaseRxHandler.DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }


    /**
     * Same as asStdOut, but buffered
     *
     * @param timeout  process timeout
     * @param timeUnit process timeout units
     * @return stdout single
     */
    public Single<byte[]> asStdOutSingle(long timeout, TimeUnit timeUnit) {
        return new AsStdOut(this).create(timeout, timeUnit)
                .toList()
                .map(listOfbyteArrays -> {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    for (byte[] arr : listOfbyteArrays) {
                        stream.write(arr);
                    }
                    return stream.toByteArray();
                });
    }

    /**
     * Same as asStdOutSingle, with default timeout. Default timeout - 60 sec, can be set via rxprocess2.defaultTimeoutMillis property
     *
     * @return stdout single
     */
    public Single<byte[]> asStdOutSingle() {
        return asStdOutSingle(BaseRxHandler.DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Represent process as all possible control flows, more @see {@link PreparedStreams}
     *
     * @return stream container
     */
    public PreparedStreams asStdInOut() {
        return new PreparedStreams(this);
    }


    /**
     * Represent process as single "done" callback
     *
     * @param timeout  process timeout
     * @param timeUnit process timeout units
     * @return single contained process exit code with optional failure
     */
    public Single<Exit> asWaitDone(long timeout, TimeUnit timeUnit) {
        return new PreparedStreams(this).waitDone(timeout, timeUnit);
    }

    /**
     * Same as waitDone with default timeout. Default timeout - 60 sec, can be set via rxprocess2.defaultTimeoutMillis property
     *
     * @return single contained process exit code with optional failure
     */
    public Single<Exit> asWaitDone() {
        return new PreparedStreams(this).waitDone();
    }

    /**
     * Create new builder with command
     *
     * @param command command
     * @return new builder
     */
    public static RxNuProcessBuilder fromCommand(List<String> command) {
        return new RxNuProcessBuilder(command);
    }

}