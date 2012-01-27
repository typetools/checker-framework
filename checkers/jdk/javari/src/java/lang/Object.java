package java.lang;
import checkers.javari.quals.*;

public class Object {

    private static native void registerNatives();
    public final native Class<?> getClass(@ReadOnly Object this);
    public native int hashCode(@ReadOnly Object this);

    public boolean equals(@ReadOnly Object this, @ReadOnly Object obj){
        throw new RuntimeException("skeleton method");
    }

    protected native Object clone(@ReadOnly Object this) throws CloneNotSupportedException ;

    public String toString(@ReadOnly Object this) {
        throw new RuntimeException("skeleton method");
    }

    public final native void notify(@ReadOnly Object this);
    public final native void notifyAll(@ReadOnly Object this);
    public final native void wait(@ReadOnly Object this, long timeout) throws InterruptedException;

    public final void wait(@ReadOnly Object this, long timeout, int nanos) throws InterruptedException  {
        throw new RuntimeException("skeleton method");
    }

    public final void wait(@ReadOnly Object this) throws InterruptedException  {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws Throwable { }
}
