package com.stericson.RootTools;

/**
 * Developers may throw this exception from within their code
 * when using IResult as a means to change the program flow.
 */
public class RootToolsException extends Exception {

    private static final long serialVersionUID = -4431771251773644144L;

    public RootToolsException(Throwable th) {
        super(th);
    }
}
