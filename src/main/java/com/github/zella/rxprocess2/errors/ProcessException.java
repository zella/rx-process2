package com.github.zella.rxprocess2.errors;

/**
 * Basic class for rxprocess2 exceptions. Msg contains stderr,
 * <p>
 * Max stderr size can be set with "rxprocess2.stderrBuffer" property, default 16384 bytes.
 * Note - buffer backed with circular fifo
 */
public class ProcessException extends RuntimeException {

    public final int exitCode;

    public ProcessException(int exitCode, String msg) {
        super(msg);
        this.exitCode = exitCode;
    }
}
