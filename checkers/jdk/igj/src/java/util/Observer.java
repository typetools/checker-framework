package java.util;
import checkers.igj.quals.*;

@I
public interface Observer{
  public abstract void update(@ReadOnly java.util.Observable a1, java.lang.Object a2) @Mutable;
}
