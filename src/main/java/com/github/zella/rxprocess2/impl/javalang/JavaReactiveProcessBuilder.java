package com.github.zella.rxprocess2.impl.javalang;

import com.github.zella.rxprocess2.BaseReactiveProcessBuilder;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.IReactiveProcess;
import com.github.zella.rxprocess2.ProcessChunk;
import com.github.zella.rxprocess2.common.ArrayUtils;
import com.github.zella.rxprocess2.common.CircularFifoQueue;
import com.github.zella.rxprocess2.common.RxUtils;
import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.zella.rxprocess2.RxProcessConfig.GRACEFULL_STOP_SECONDS;
import static com.github.zella.rxprocess2.RxProcessConfig.STDERR_BUFF_SIZE;


public class JavaReactiveProcessBuilder extends BaseReactiveProcessBuilder<Process> {

    private final ProcessBuilder builder;

    public JavaReactiveProcessBuilder(ProcessBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Single<Exit> asWaitDone(long timeout, TimeUnit timeUnit) {
        return Single.<Exit>create(emitter -> {

            Collection<Byte> stderrBuffer = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

            Process process = builder.start();

            emitter.setCancellable(destroyProcess(process));

            InputStream stderr = process.getErrorStream();

            CountDownLatch waitOut = new CountDownLatch(1);

            RxUtils.bytes(stderr)
                    .doFinally(waitOut::countDown)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(b -> {
                        synchronized (emitter) {
                            for (byte b1 : b) {
                                stderrBuffer.add(b1);
                            }
                        }
                    }, err -> {
                    }, () -> {
                    });

            if (stdin.length > 0) {
                OutputStream procStdin = process.getOutputStream();
                procStdin.write(stdin);
                procStdin.close();
            }

            if (timeout != -1)
                waitOut.await(timeout, timeUnit);
            else
                waitOut.await();

            int exitValue = process.waitFor();

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
            if (timeout == -1) return s;
            else
                return s.timeout(timeout, timeUnit).onErrorReturn(e -> new Exit(Integer.MIN_VALUE, (e instanceof TimeoutException)
                        ? new ProcessTimeoutException(Integer.MIN_VALUE)
                        : new ProcessException(Integer.MIN_VALUE, e.getMessage()))
                );
        });

    }

    @Override
    public IReactiveProcess<Process> biDirectional() {
        return new JavaReactiveProcess(builder);
    }

    private Cancellable destroyProcess(Process process) {
        return () -> {
            if (GRACEFULL_STOP_SECONDS == -1) {
                process.destroyForcibly();
            } else {
                process.destroy();
                if (!process.waitFor(GRACEFULL_STOP_SECONDS, TimeUnit.SECONDS))
                    process.destroyForcibly();
            }
        };
    }

    public Observable<ProcessChunk> asStdErrOut(long timeout, TimeUnit timeUnit) {
        return Observable.<ProcessChunk>create(
                emitter -> {
                    Collection<Byte> stderrBuffer = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

                    Process process = builder.start();

                    InputStream stdout = process.getInputStream();
                    InputStream stderr = process.getErrorStream();

                    CountDownLatch waitOut = new CountDownLatch(2);

                    emitter.setCancellable(destroyProcess(process));

                    RxUtils.bytes(stdout)
                            .doFinally(waitOut::countDown)
                            .subscribeOn(Schedulers.newThread())
                            .subscribe(b -> {
                                synchronized (emitter) {
                                    emitter.onNext(new ProcessChunk(b, false));
                                }
                            }, err -> {
                            }, () -> {
                            });

                    RxUtils.bytes(stderr)
                            .doFinally(waitOut::countDown)
                            .subscribeOn(Schedulers.newThread())
                            .subscribe(b -> {
                                synchronized (emitter) {
                                    emitter.onNext(new ProcessChunk(b, true));
                                    for (byte b1 : b) {
                                        stderrBuffer.add(b1);
                                    }
                                }
                            }, err -> {
                            }, () -> {
                            });

                    if (stdin.length > 0) {
                        OutputStream procStdin = process.getOutputStream();
                        procStdin.write(stdin);
                        procStdin.close();
                    }

                    if (timeout != -1)
                        waitOut.await(timeout, timeUnit);
                    else
                        waitOut.await();

                    int exitValue = process.waitFor();

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

                })
                .compose(o -> {
                    if (timeout == -1)
                        return o;
                    else return o.takeUntil(Observable.timer(timeout, timeUnit).map(bytes -> {
                        throw new ProcessTimeoutException(Integer.MIN_VALUE);
                    }));
                });
    }

}
