package com.github.zella.rxprocess2.impl.javalang;

import com.github.zella.rxprocess2.BaseReactiveProcess;
import com.github.zella.rxprocess2.Exit;
import com.github.zella.rxprocess2.ProcessChunk;
import com.github.zella.rxprocess2.common.ArrayUtils;
import com.github.zella.rxprocess2.common.RxUtils;
import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import io.reactivex.Single;
import io.reactivex.functions.Cancellable;
import io.reactivex.schedulers.Schedulers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.zella.rxprocess2.RxProcessConfig.GRACEFULL_STOP_SECONDS;

public class JavaReactiveProcess extends BaseReactiveProcess<Process> {


    private final ProcessBuilder builder;

    JavaReactiveProcess(ProcessBuilder builder) {
        this.builder = builder;

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


    @Override
    public Single<Exit> waitDone(long timeout, TimeUnit timeUnit) {
        return Single.<Exit>create(emitter -> {

            Process process = builder.start();

            startedSubject.onNext(process);
            startedSubject.onComplete();

            CountDownLatch waitOut = new CountDownLatch(2);

            emitter.setCancellable(destroyProcess(process));

            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();

            stdinProcessor
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(bytes -> {
                                stdin.write(bytes);
                                stdin.flush();
                            },
                            err -> process.destroyForcibly(),
                            stdin::close);

            RxUtils.bytes(stderr)
                    .doFinally(waitOut::countDown)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(b -> {
                        synchronized (emitter) {
                            //backpressure should be ok
                            stdoutStdErrSubject.onNext(new ProcessChunk(b, true));
                            for (byte b1 : b) {
                                stderrBuffer.add(b1);
                            }
                        }
                    }, err -> {

                    }, () -> {

                    });

            RxUtils.bytes(stdout)
                    .doFinally(waitOut::countDown)
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(b -> {
                        synchronized (emitter) {
                            stdoutStdErrSubject.onNext(new ProcessChunk(b, false));
                        }
                    }, err -> {
                    }, () -> {
                    });

            if (timeout != -1)
                waitOut.await(timeout, timeUnit);
            else
                waitOut.await();


            int exitValue = process.waitFor();
            synchronized (emitter) {
                if (exitValue != 0) {
                    String err = new String(ArrayUtils.toPrimitive(stderrBuffer.toArray(new Byte[0])));
                    stdoutStdErrSubject.onError(new ProcessException(exitValue, err));
                    if ((!emitter.isDisposed())) {
                        emitter.onSuccess(new Exit(exitValue, new ProcessException(exitValue, err)));
                    }
                } else {
                    stdoutStdErrSubject.onComplete();
                    if ((!emitter.isDisposed())) {
                        emitter.onSuccess(new Exit(0));
                    }
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


}
