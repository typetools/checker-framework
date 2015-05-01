package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;

public class AssertionError extends Error {
  private static final long serialVersionUID = 0;

    public AssertionError() {
    }

    private AssertionError(@Nullable String detailMessage) {
        super(detailMessage);
    }

    public AssertionError(@Nullable Object detailMessage) {
        this("" +  detailMessage);
        if (detailMessage instanceof Throwable)
            initCause((Throwable) detailMessage);
    }

    public AssertionError(boolean detailMessage) {
        this("" +  detailMessage);
    }

    public AssertionError(char detailMessage) {
        this("" +  detailMessage);
    }

    public AssertionError(int detailMessage) {
        this("" +  detailMessage);
    }

    public AssertionError(long detailMessage) {
        this("" +  detailMessage);
    }

    public AssertionError(float detailMessage) {
        this("" +  detailMessage);
    }

    public AssertionError(double detailMessage) {
        this("" +  detailMessage);
    }
}
