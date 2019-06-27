package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.errors.ProcessException;
import com.github.zella.rxprocess2.etc.ArrayUtils;
import com.github.zella.rxprocess2.etc.CircularFifoQueue;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.annotations.NonNull;
import io.reactivex.processors.ReplayProcessor;
import io.reactivex.subjects.ReplaySubject;

import java.nio.ByteBuffer;
import java.util.Collection;

abstract class BaseRxHandler extends NuAbstractProcessHandler {

    //no timeout by default
    static int DEFAULT_PROCESS_TIMEOUT_MILLIS = Integer.getInteger("rxprocess2.defaultTimeoutMillis", -1);

    private static int STDERR_BUFF_SIZE = Integer.getInteger("rxprocess2.stderrBuffer", 16384);

    final ReplayProcessor<byte[]> rxIn = ReplayProcessor.create();

    abstract void onNext(@NonNull byte[] value);

    abstract void onError(int code);

    abstract void onSuccesfullComplete();

    private Collection<Byte> stderr = new CircularFifoQueue<>(STDERR_BUFF_SIZE);

    @Override
    public void onStart(NuProcess nuProcess) {
        rxIn.subscribe(
                bytes -> nuProcess.writeStdin(ByteBuffer.wrap(bytes)),
                err -> nuProcess.destroy(true),
                () -> nuProcess.closeStdin(false));
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        if (!closed) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            onNext(bytes);
        }
    }

    @Override
    public void onStderr(ByteBuffer buffer, boolean closed) {
        if (!closed) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            synchronized (this) {
                for (byte b : bytes) {
                    stderr.add(b);
                }
            }
        }
    }

    ProcessException error(int code, String err) {
        return new ProcessException(code, err);
    }

    String getErr() {
        synchronized (this) {
            return new String(ArrayUtils.toPrimitive(stderr.toArray(new Byte[0])));
        }
    }

    @Override
    public void onExit(int statusCode) {
        if (statusCode != 0)
            onError(statusCode);
        else
            onSuccesfullComplete();
    }
}
