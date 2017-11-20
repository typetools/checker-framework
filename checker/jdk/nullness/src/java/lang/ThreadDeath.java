package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class ThreadDeath extends Error{
    private static final long serialVersionUID = 0L;
  @SideEffectFree
  public ThreadDeath() { throw new RuntimeException("skeleton method"); }
}
