package com.github.zella.rxprocess2;


import com.github.zella.rxprocess2.errors.ProcessException;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents successful or not process exit
 */
public class Exit {
    public final int statusCode;
    public final Optional<ProcessException> err;

    public  Exit(int statusCode, ProcessException err) {
        this.statusCode = statusCode;
        this.err = Optional.of(err);
    }

    public Exit(int statusCode) {
        this.statusCode = statusCode;
        this.err = Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exit exit = (Exit) o;
        return statusCode == exit.statusCode &&
                Objects.equals(err, exit.err);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, err);
    }

    @Override
    public String toString() {
        return "Exit{" +
                "statusCode=" + statusCode +
                ", err=" + err +
                '}';
    }
}