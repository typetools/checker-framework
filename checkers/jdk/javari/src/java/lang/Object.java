package java.lang;
import checkers.javari.quals.*;

public class Object {

    private static native void registerNatives();
    public final native Class<?> getClass() @ReadOnly;
    public native int hashCode() @ReadOnly;

    public boolean equals(@ReadOnly Object obj) @ReadOnly{
        throw new RuntimeException("skeleton method");
    }

    protected native Object clone() @ReadOnly throws CloneNotSupportedException ;

    public String toString() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public final native void notify() @ReadOnly;
    public final native void notifyAll() @ReadOnly;
    public final native void wait(long timeout) @ReadOnly throws InterruptedException;

    public final void wait(long timeout, int nanos) @ReadOnly throws InterruptedException  {
        throw new RuntimeException("skeleton method");
    }

    public final void wait() @ReadOnly throws InterruptedException  {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws Throwable { }
}
