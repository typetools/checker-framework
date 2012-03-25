package java.util;
import checkers.igj.quals.*;

@I
public interface Observer{
  public abstract void update(@Mutable Observer this, @ReadOnly Observable a1, Object a2);
}
