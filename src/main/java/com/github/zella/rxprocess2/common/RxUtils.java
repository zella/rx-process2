package com.github.zella.rxprocess2.common;

import com.github.davidmoten.rx2.Bytes;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Function;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

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

}
