package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.PublishSubject;
import org.reactivestreams.Subscriber;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents process as all possible control flows
 */
public class PreparedStreams {

    private final AsyncSubject<NuProcess> startedH = AsyncSubject.create();
    private final HotStdoutHandler handler = new HotStdoutHandler();
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
    public Single<Exit> waitDone(long timeout, TimeUnit timeUnit) {
        return Single.<Exit>create(emitter -> {

            LinkedBlockingQueue<Exit> switchThread = new LinkedBlockingQueue<>();

            handler.setQueue(switchThread);
            pb.builder.setProcessListener(handler);
            NuProcess p = pb.builder.start();
            emitter.setCancellable(() -> p.destroy(true));
            startedH.onNext(p);
            startedH.onComplete();

            Exit n = switchThread.take();
            if (!emitter.isDisposed())
                emitter.onSuccess(n);

            //timeout handled by rx
        }).compose(s -> {
            if (timeout == -1) return s;
            else
                return s.timeout(timeout, timeUnit).onErrorReturn(e -> new Exit(Integer.MIN_VALUE, (e instanceof TimeoutException)
                        ? new ProcessTimeoutException(Integer.MIN_VALUE)
                        : new ProcessException(Integer.MIN_VALUE, e.getMessage()))
                );
        });
    }

    /**
     * Same as {@link #waitDone(long, TimeUnit)} with default timeout. Default timeout -1, means no timeout, can be set via rxprocess2.defaultTimeoutMillis property
     *
     * @return single contained process exit code with optional failure
     */
    public Single<Exit> waitDone() {
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
    public Subscriber<byte[]> stdIn() {
        return handler.rxIn;
    }

    /**
     * Stdout hot "callback"
     *
     * @return Process stdout.
     */
    public Observable<byte[]> stdOut() {
        return handler.rxOut;
    }


    static class HotStdoutHandler extends BaseRxHandler {

        private BlockingQueue<Exit> queue = null;

        final PublishSubject<byte[]> rxOut = PublishSubject.create();

        void setQueue(BlockingQueue<Exit> queue) {
            this.queue = queue;
        }

        @Override
        void onNext(byte[] value) {
            //note called on nuprocess thread
            rxOut.onNext(value);
        }

        @Override
        void onError(int code) {
            rxOut.onError(error(code, getErr()));
            queue.add(new Exit(code, new ProcessException(code, getErr())));
        }

        @Override
        void onSuccesfullComplete() {
            rxOut.onComplete();
            queue.add(new Exit(0));
        }
    }

}

