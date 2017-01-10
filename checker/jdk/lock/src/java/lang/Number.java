package java.lang;

import org.checkerframework.checker.lock.qual.*;

public abstract class Number implements java.io.Serializable {
  public Number() { throw new RuntimeException("skeleton method"); }
    private static final long serialVersionUID = -8742448824652078965L;
   public abstract int intValue(@GuardSatisfied Number this);
   public abstract long longValue(@GuardSatisfied Number this);
   public abstract float floatValue(@GuardSatisfied Number this);
   public abstract double doubleValue(@GuardSatisfied Number this);
   public byte byteValue(@GuardSatisfied Number this) { throw new RuntimeException("skeleton method"); }
   public short shortValue(@GuardSatisfied Number this) { throw new RuntimeException("skeleton method"); }
}
