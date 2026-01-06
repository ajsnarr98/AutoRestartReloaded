package com.github.ajsnarr98.autorestartreloaded.core.error;

public class InvalidRestartTimeException extends IllegalArgumentException {
    public InvalidRestartTimeException(String msg) {
        super(msg);
    }

    public InvalidRestartTimeException(Throwable cause) {
        super(cause);
    }

    public InvalidRestartTimeException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
