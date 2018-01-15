package java.lang;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AssertionError extends Error {
  private static final long serialVersionUID = 0;

    @SideEffectFree
    public AssertionError() {
    }

    @SideEffectFree
    private AssertionError(@Nullable String detailMessage) {
        super(detailMessage);
    }

    @SideEffectFree
    public AssertionError(@Nullable Object detailMessage) {
        this("" +  detailMessage);
        if (detailMessage instanceof Throwable)
            initCause((Throwable) detailMessage);
    }

    @SideEffectFree
    public AssertionError(boolean detailMessage) {
        this("" +  detailMessage);
    }

    @SideEffectFree
    public AssertionError(char detailMessage) {
        this("" +  detailMessage);
    }

    @SideEffectFree
    public AssertionError(int detailMessage) {
        this("" +  detailMessage);
    }

    @SideEffectFree
    public AssertionError(long detailMessage) {
        this("" +  detailMessage);
    }

    @SideEffectFree
    public AssertionError(float detailMessage) {
        this("" +  detailMessage);
    }

    @SideEffectFree
    public AssertionError(double detailMessage) {
        this("" +  detailMessage);
    }
}
