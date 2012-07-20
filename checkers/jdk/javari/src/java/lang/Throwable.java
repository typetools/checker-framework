package java.lang;
import checkers.javari.quals.*;
import  java.io.*;

public class Throwable implements Serializable {

    private static final long serialVersionUID = -3042686055658047285L;
    private transient Object backtrace;
    private String detailMessage;
    private Throwable cause = this;
    private StackTraceElement[] stackTrace;

    public Throwable() {
        throw new RuntimeException("skeleton method");
    }

    public Throwable(String message) {
        throw new RuntimeException("skeleton method");
    }

    public Throwable(@PolyRead Throwable this, String message, @PolyRead Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public Throwable(@PolyRead Throwable this, @PolyRead Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public String getMessage(@ReadOnly Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    public String getLocalizedMessage(@ReadOnly Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    public @PolyRead Throwable getCause(@PolyRead Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized Throwable initCause(Throwable cause) {
        throw new RuntimeException("skeleton method");
    }

    public String toString(@ReadOnly Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    public void printStackTrace(@ReadOnly Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    public void printStackTrace(@ReadOnly Throwable this, PrintStream s) {
        throw new RuntimeException("skeleton method");
    }

    private void printStackTraceAsCause(PrintStream s, StackTraceElement[] causedTrace) {
        throw new RuntimeException("skeleton method");
    }

    public void printStackTrace(@ReadOnly Throwable this, PrintWriter s) {
        throw new RuntimeException("skeleton method");
    }

    private void printStackTraceAsCause(PrintWriter s, StackTraceElement[] causedTrace) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized native Throwable fillInStackTrace();

    public StackTraceElement[] getStackTrace(@ReadOnly Throwable this) {
        throw new RuntimeException("skeleton method");
    }

    private synchronized StackTraceElement[] getOurStackTrace() {
        throw new RuntimeException("skeleton method");
    }

    public void setStackTrace(StackTraceElement @ReadOnly [] stackTrace) {
        throw new RuntimeException("skeleton method");
    }

    private native int getStackTraceDepth(@ReadOnly Throwable this);
    private native @PolyRead StackTraceElement getStackTraceElement(@PolyRead Throwable this, int index);

    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
