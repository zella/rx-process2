package com.github.zella.rxprocess2.common;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static com.github.zella.rxprocess2.RxProcessConfig.DEFAULT_READ_BUFFER;

public class RxUtils {
    public static Single<byte[]> collect(Observable<byte[]> source) {
        return source.collect(BosCreatorHolder.INSTANCE, BosCollectorHolder.INSTANCE).map(BosToArrayHolder.INSTANCE);
    }

    private static final class BosCreatorHolder {
        static final Callable<ByteArrayOutputStream> INSTANCE = new Callable<ByteArrayOutputStream>() {

            @Override
            public ByteArrayOutputStream call() {
                return new ByteArrayOutputStream();
            }
        };
    }

    private static final class BosCollectorHolder {
        static final BiConsumer<ByteArrayOutputStream, byte[]> INSTANCE = new BiConsumer<ByteArrayOutputStream, byte[]>() {

            @Override
            public void accept(ByteArrayOutputStream bos, byte[] bytes) throws IOException {
                bos.write(bytes);
            }
        };
    }

    private static final class BosToArrayHolder {
        static final Function<ByteArrayOutputStream, byte[]> INSTANCE = new Function<ByteArrayOutputStream, byte[]>() {
            @Override
            public byte[] apply(ByteArrayOutputStream bos) {
                return bos.toByteArray();
            }
        };
    }

    public static Observable<byte[]> bytes(final InputStream is) {
        return Observable.generate(emitter -> {
            byte[] buffer = new byte[DEFAULT_READ_BUFFER];
            int count = is.read(buffer);
            if (count == -1) {
                emitter.onComplete();
            } else if (count < DEFAULT_READ_BUFFER) {
                emitter.onNext(Arrays.copyOf(buffer, count));
            } else {
                emitter.onNext(buffer);
            }
        });
    }

}
