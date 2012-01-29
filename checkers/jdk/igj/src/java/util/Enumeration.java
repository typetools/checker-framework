package java.util;
import checkers.igj.quals.*;

@Immutable
public interface Enumeration<E> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
