package com.github.zella.rxprocess2.impl.nuprocess;

import com.github.zella.rxprocess2.ProcessChunk;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.annotations.NonNull;
import io.reactivex.processors.ReplayProcessor;

import java.nio.ByteBuffer;

abstract class BaseNuProcessHandler extends NuAbstractProcessHandler {

    final ReplayProcessor<byte[]> rxIn = ReplayProcessor.create();

    abstract void onNext(@NonNull ProcessChunk chunk);

    abstract void onComplete(int code);

    abstract void started(NuProcess nuProcess);

    abstract void stdoutClosed();

    abstract void stderrClosed();

    @Override
    public void onStart(NuProcess nuProcess) {
        started(nuProcess);
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        synchronized (this) {
            if (!closed) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                onNext(new ProcessChunk(bytes, false));
            } else {
                stdoutClosed();
            }
        }
    }

    @Override
    public void onStderr(ByteBuffer buffer, boolean closed) {
        synchronized (this) {
            if (!closed) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                onNext(new ProcessChunk(bytes, true));
            } else {
                stderrClosed();
            }
        }
    }


    @Override
    public void onExit(int statusCode) {
        onComplete(statusCode);
    }
}
