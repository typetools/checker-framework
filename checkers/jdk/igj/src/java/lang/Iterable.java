package java.lang;
import checkers.igj.quals.*;

@I
public interface Iterable<T> {
    public abstract @I java.util.Iterator<T> iterator(@ReadOnly Iterable<T> this);
}
