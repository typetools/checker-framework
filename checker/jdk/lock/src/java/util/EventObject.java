package java.util;
import org.checkerframework.checker.lock.qual.*;

public class EventObject implements java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public EventObject(Object a1) { throw new RuntimeException("skeleton method"); }
  public Object getSource(@GuardSatisfied EventObject this) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied EventObject this) { throw new RuntimeException("skeleton method"); }
}
