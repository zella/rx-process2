package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.PublishSubject;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PreparedStreams {

    private final AsyncSubject<NuProcess> startedH = AsyncSubject.create();
    private HotStdoutHandler handler = new HotStdoutHandler();
    private final RxNuProcessBuilder pb;

    PreparedStreams(RxNuProcessBuilder builder) {
        this.pb = builder;
    }

    /**
     * Represent process as single "done" callback. To start process you should subscribe to this
     *
     * @param timeout  process timeout
     * @param timeUnit process timeout units
     * @return single contained process exit code with optional failure
     */
    Single<Exit> waitDone(long timeout, TimeUnit timeUnit) {
        return Single.<Exit>create(emitter -> {
            pb.builder.setProcessListener(handler);
            NuProcess p = pb.builder.start();
            emitter.setCancellable(() -> p.destroy(true));
            startedH.onNext(p);
            startedH.onComplete();
            //timeout handled by rx
            int code = p.waitFor(0, timeUnit);
            if (code == 0) {
                emitter.onSuccess(new Exit(code));
            } else {
                emitter.onSuccess(new Exit(code, new ProcessException(code, handler.getErr())));
            }
        }).timeout(timeout, timeUnit).onErrorReturn(e -> new Exit(Integer.MIN_VALUE, (e instanceof TimeoutException)
                ? new ProcessTimeoutException(Integer.MIN_VALUE)
                : new ProcessException(Integer.MIN_VALUE, e.getMessage())
        ));
    }

    /**
     * Same as waitDone ith default timeout. Default timeout - 60 sec, can be set via rxprocess2.defaultTimeoutMillis property
     *
     * @return single contained process exit code with optional failure
     */
    Single<Exit> waitDone() {
        return waitDone(BaseRxHandler.DEFAULT_PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Process started hot "callback"
     *
     * @return Process started "callback"
     */
    public Single<NuProcess> started() {
        return startedH.firstOrError();
    }

    /**
     * Process stdin. Send Error to force kill process, Complete - close stdin pipe
     *
     * @return Process stdin.
     */
    public Observer<byte[]> stdIn() {
        return handler.rxIn;
    }

    /**
     * Stdout hot "callback"
     *
     * @return Process stdin.
     */
    public Observable<byte[]> stdOut() {
        return handler.rxOut;
    }


    static class HotStdoutHandler extends BaseRxHandler {

        final PublishSubject<byte[]> rxOut = PublishSubject.create();

        @Override
        void onNext(byte[] value) {
            rxOut.onNext(value);
        }

        @Override
        void onError(ProcessException error) {
            rxOut.onError(error);
        }

        @Override
        void onComplete() {
            rxOut.onComplete();
        }
    }

}
