package java.io;

import org.checkerframework.checker.lock.qual.*;


public class ObjectStreamClass implements Serializable {
  private static final long serialVersionUID = 0;
  protected ObjectStreamClass() {}
  public final static ObjectStreamField[] NO_FIELDS = {};
  public static ObjectStreamClass lookup(Class<?> a1) { throw new RuntimeException("skeleton method"); }
  public static ObjectStreamClass lookupAny(Class<?> a1) { throw new RuntimeException("skeleton method"); }
  public String getName() { throw new RuntimeException("skeleton method"); }
  public long getSerialVersionUID() { throw new RuntimeException("skeleton method"); }
  public Class<?> forClass() { throw new RuntimeException("skeleton method"); }
  public ObjectStreamField[] getFields() { throw new RuntimeException("skeleton method"); }
  public ObjectStreamField getField(String a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied ObjectStreamClass this) { throw new RuntimeException("skeleton method"); }
}
