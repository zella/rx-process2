package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.errors.ProcessTimeoutException;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import java.util.concurrent.TimeUnit;

class AsStdOut {

    private final RxNuProcessBuilder pb;

    AsStdOut(RxNuProcessBuilder pb) {
        this.pb = pb;
    }

    Observable<byte[]> create(long timeout, TimeUnit timeUnit) {
        return Observable.create((ObservableOnSubscribe<byte[]>) emitter -> {
            ColdStdoutHandler handler = new ColdStdoutHandler(emitter);
            pb.builder.setProcessListener(handler);
            NuProcess p = pb.builder.start();
            emitter.setCancellable(() -> p.destroy(true));
            //timeout handled by rx
            int code = p.waitFor(0, timeUnit);
            if (!emitter.isDisposed())
                if (code == 0) {
                    emitter.onComplete();
                } else {
                    emitter.onError(new ProcessException(code, handler.getErr()));
                }
        }).compose(o -> {
                    if (timeout == -1)
                        return o;
                    else return o.takeUntil(Observable.timer(timeout, timeUnit).map(bytes -> {
                        throw new ProcessTimeoutException(Integer.MIN_VALUE);
                    }));
                }

        );
    }

    static class ColdStdoutHandler extends BaseRxHandler {

        final ObservableEmitter<byte[]> rxOut;

        ColdStdoutHandler(ObservableEmitter<byte[]> rxOut) {
            this.rxOut = rxOut;
        }

        @Override
        void onNext(byte[] value) {
            rxOut.onNext(value);
        }

        @Override
        void onError(ProcessException error) {
            //do nothing
        }

        @Override
        void onComplete() {
            //do nothing
        }

    }
}