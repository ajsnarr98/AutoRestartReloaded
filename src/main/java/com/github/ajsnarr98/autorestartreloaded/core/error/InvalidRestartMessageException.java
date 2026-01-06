package com.github.ajsnarr98.autorestartreloaded.core.error;

public class InvalidRestartMessageException extends IllegalArgumentException {
    public InvalidRestartMessageException(String msg) {
        super(msg);
    }

    public InvalidRestartMessageException(Throwable cause) {
        super(cause);
    }

    public InvalidRestartMessageException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
