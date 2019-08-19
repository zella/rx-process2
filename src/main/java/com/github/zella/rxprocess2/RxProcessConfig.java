package com.github.zella.rxprocess2;

public final class RxProcessConfig {

    private RxProcessConfig() {
    }

    public static final int STDERR_BUFF_SIZE = Integer.getInteger("rxprocess2.stderrBuffer", 4096);

    public static final int GRACEFULL_STOP_SECONDS = Integer.getInteger("rxprocess2.gracefullStopSeconds", 1);

    public static final int DEFAULT_PROCESS_TIMEOUT_MILLIS = Integer.getInteger("rxprocess2.timeOutMillis", -1);

    public static final int DEFAULT_READ_BUFFER = Integer.getInteger("rxprocess2.readBuffer", 8192);

}
