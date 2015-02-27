package java.util;
import org.checkerframework.checker.igj.qual.*;

@Immutable
public interface Enumeration<E> {
  public abstract boolean hasMoreElements();
  public abstract E nextElement();
}
