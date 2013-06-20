package java.util;
import dataflow.quals.Pure;
import checkers.nullness.quals.Nullable;

public class EventObject implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public EventObject(Object a1) { throw new RuntimeException("skeleton method"); }
  public Object getSource() { throw new RuntimeException("skeleton method"); }
  @Pure public String toString() { throw new RuntimeException("skeleton method"); }
}
