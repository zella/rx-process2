package com.github.zella.rxprocess2.impl.nuprocess;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import com.github.zella.rxprocess2.common.ArrayUtils;
import com.github.zella.rxprocess2.common.CircularFifoQueue;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.IReactiveProcess;
import com.github.zella.rxprocess2.ProcessChunk;
import com.github.zella.rxprocess2.BaseReactiveProcessBuilder;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.zella.rxprocess2.RxProcessConfig.GRACEFULL_STOP_SECONDS;
import static com.github.zella.rxprocess2.RxProcessConfig.STDERR_BUFF_SIZE;

public class NuReactiveProcessBuilder extends BaseReactiveProcessBuilder<NuProcess> {

    private final NuProcessBuilder builder;

    public NuReactiveProcessBuilder(NuProcessBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Single<Exit> asWaitDone(long timeout, TimeUnit timeUnit) {
        return Single.<Exit>create(emitter -> {

            Collection<Byte> stderrBuffer = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

            builder.setProcessListener(new BaseNuProcessHandler() {
                @Override
                void onNext(ProcessChunk chunk) {

                    if (chunk.isStdErr) {
                        for (byte b1 : chunk.data) {
                            stderrBuffer.add(b1);
                        }
                    }
                }

                @Override
                void onComplete(int code) {
                }

                @Override
                void started(NuProcess nuProcess) {
                    if (stdin.length > 0) {
                        nuProcess.writeStdin(ByteBuffer.wrap(stdin));
                        nuProcess.closeStdin(false);
                    }
                }

                @Override
                void stdoutClosed() {
                }

                @Override
                void stderrClosed() {
                }
            });

            NuProcess process = builder.start();

            emitter.setCancellable(() -> {
                if (GRACEFULL_STOP_SECONDS == -1) {
                    process.destroy(true);
                } else {
                    try {
                        process.destroy(false);
                        process.waitFor(GRACEFULL_STOP_SECONDS, TimeUnit.SECONDS);
                    } finally {
                        if (process.isRunning())
                            process.destroy(true);
                    }
                }
            });

            //INFINITY
            int exitValue = process.waitFor(0, TimeUnit.SECONDS);

            if (!emitter.isDisposed()) {
                if (exitValue != 0) {
                    synchronized (emitter) {
                        String err = new String(ArrayUtils.toPrimitive(stderrBuffer.toArray(new Byte[0])));
                        emitter.onSuccess(new Exit(exitValue, new ProcessException(exitValue, err)));
                    }
                } else {
                    emitter.onSuccess(new Exit(0));
                }
            }

        }).compose(s -> {
            //TODO revision
            if (timeout == -1) return s;
            else
                return s.timeout(timeout, timeUnit).onErrorReturn(e -> new Exit(Integer.MIN_VALUE, (e instanceof TimeoutException)
                        ? new ProcessTimeoutException(Integer.MIN_VALUE)
                        : new ProcessException(Integer.MIN_VALUE, e.getMessage()))
                );
        });
    }

    @Override
    public Observable<ProcessChunk> asStdErrOut(long timeout, TimeUnit timeUnit) {
        return Observable.<ProcessChunk>create(emitter -> {

            Collection<Byte> stderrBuffer = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

            builder.setProcessListener(new BaseNuProcessHandler() {
                @Override
                void onNext(ProcessChunk chunk) {
                    emitter.onNext(chunk);
                    if (chunk.isStdErr) {
                        for (byte b1 : chunk.data) {
                            stderrBuffer.add(b1);
                        }
                    }
                }

                @Override
                void onComplete(int code) {
                }

                @Override
                void started(NuProcess nuProcess) {
                    if (stdin.length > 0) {
                        nuProcess.writeStdin(ByteBuffer.wrap(stdin));
                        nuProcess.closeStdin(false);
                    }
                }

                @Override
                void stdoutClosed() {
                }

                @Override
                void stderrClosed() {
                }
            });

            NuProcess process = builder.start();

            emitter.setCancellable(() -> {
                if (GRACEFULL_STOP_SECONDS == -1) {
                    process.destroy(true);
                } else {
                    try {
                        process.destroy(false);
                        process.waitFor(GRACEFULL_STOP_SECONDS, TimeUnit.SECONDS);
                    } finally {
                        if (process.isRunning())
                            process.destroy(true);
                    }
                }
            });

            //INFINITY
            int exitValue = process.waitFor(0, TimeUnit.SECONDS);

            if (!emitter.isDisposed()) {
                if (exitValue != 0) {
                    synchronized (emitter) {
                        String err = new String(ArrayUtils.toPrimitive(stderrBuffer.toArray(new Byte[0])));
                        emitter.onError(new ProcessException(exitValue, err));
                    }
                } else {
                    emitter.onComplete();
                }
            }

        }).compose(o -> {
            if (timeout == -1)
                return o;
            else return o.takeUntil(Observable.timer(timeout, timeUnit).map(bytes -> {
                throw new ProcessTimeoutException(Integer.MIN_VALUE);
            }));
        });
    }


    @Override
    public IReactiveProcess<NuProcess> biDirectional() {
        return new NuReactiveProcess(builder);
    }
}
