package java.util;
import checkers.igj.quals.*;

@I
public interface Observer{
  public abstract void update(@ReadOnly Observable a1, Object a2) @Mutable;
}
