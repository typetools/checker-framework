package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public interface Observer{
  public abstract void update(@Mutable Observer this, @ReadOnly Observable a1, Object a2);
}
