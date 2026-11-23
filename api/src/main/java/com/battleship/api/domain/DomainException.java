package com.battleship.api.domain;

/** Business-rule violation. The web layer maps Code.httpStatus() to the HTTP response. */
public class DomainException extends RuntimeException {

    public enum Code {
        GAME_NOT_FOUND(404),
        FORBIDDEN(403),
        ILLEGAL_STATE(409),
        NOT_YOUR_TURN(409),
        ALREADY_SHOT(409),
        INVALID_PLACEMENT(422),
        INVALID_SHOT(422);

        private final int httpStatus;

        Code(int httpStatus) { this.httpStatus = httpStatus; }

        public int httpStatus() { return httpStatus; }
    }

    private final Code code;

    public DomainException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() { return code; }
}
