package java.lang;
import org.checkerframework.checker.igj.qual.*;

@I
public interface Iterable<T extends @ReadOnly Object> {
    public abstract java.util. @I Iterator<T> iterator(@ReadOnly Iterable<T> this);
}
