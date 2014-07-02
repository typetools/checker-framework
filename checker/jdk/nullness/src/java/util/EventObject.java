package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EventObject implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public EventObject(Object a1) { throw new RuntimeException("skeleton method"); }
  public Object getSource() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
}
