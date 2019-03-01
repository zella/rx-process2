package com.github.zella.rxprocess2.errors;

public class ProcessTimeoutException extends ProcessException {

    public ProcessTimeoutException(int exitCode) {
        super(exitCode, "Process timeout");
    }
}
