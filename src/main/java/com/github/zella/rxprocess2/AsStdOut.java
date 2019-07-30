package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class AsStdOut {

    private final RxNuProcessBuilder pb;

    AsStdOut(RxNuProcessBuilder pb) {
        this.pb = pb;
    }

    Observable<byte[]> create(long timeout, TimeUnit timeUnit) {
        return Observable.create((ObservableOnSubscribe<byte[]>) emitter -> {

            LinkedBlockingQueue<Notification<byte[]>> switchThread = new LinkedBlockingQueue<>();

            ColdStdoutHandler handler = new ColdStdoutHandler(switchThread);
            pb.builder.setProcessListener(handler);
            NuProcess p = pb.builder.start();
            emitter.setCancellable(() -> {
                try {
                    p.destroy(false);
                    p.waitFor(BaseRxHandler.GRACEFULL_STOP_SECONDS, TimeUnit.SECONDS);
                    p.destroy(true);
                } finally {
                    Single.timer(1, TimeUnit.SECONDS)
                            .map(l -> switchThread.offer(Notification.createOnError(new ProcessException(-1, "Nu process callback missing, clean by rx"))))
                            .subscribe();
                }
            });
            while (true) {
                Notification<byte[]> n = switchThread.take();
                if (n.isOnNext()) {
                    emitter.onNext(n.getValue());

                } else if (n.isOnComplete()) {
                    if (!emitter.isDisposed())
                        emitter.onComplete();
                    break;
                } else if (n.isOnError()) {
                    if (!emitter.isDisposed())
                        emitter.onError(n.getError());
                    break;
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


    static class ColdStdoutHandler extends BaseRxHandler {

        final BlockingQueue<Notification<byte[]>> queue;

        ColdStdoutHandler(BlockingQueue<Notification<byte[]>> queue) {
            this.queue = queue;
        }

        @Override
        void onNext(byte[] value) {
            queue.add(Notification.createOnNext(value));
        }

        @Override
        void onError(int code) {
            queue.add(Notification.createOnError(error(code, getErr())));
        }

        @Override
        void onSuccesfullComplete() {
            queue.add(Notification.createOnComplete());
        }

    }
}