package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class EventObject implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public EventObject(Object a1) { throw new RuntimeException("skeleton method"); }
  public Object getSource() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
