package java.util;

import org.checkerframework.checker.lock.qual.GuardSatisfied;

public abstract class ListResourceBundle extends ResourceBundle {
  public ListResourceBundle() { throw new RuntimeException("skeleton method"); }
  public final Object handleGetObject(String a1) { throw new RuntimeException("skeleton method"); }
  public Enumeration<String> getKeys(@GuardSatisfied ListResourceBundle this) { throw new RuntimeException("skeleton method"); }
}
