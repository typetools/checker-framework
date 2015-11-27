package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.lock.qual.*;

public abstract class Number implements java.io.Serializable {
  public Number() { throw new RuntimeException("skeleton method"); }
    private static final long serialVersionUID = -8742448824652078965L;
  @Pure public abstract int intValue(@GuardSatisfied Number this);
  @Pure public abstract long longValue(@GuardSatisfied Number this);
  @Pure public abstract float floatValue(@GuardSatisfied Number this);
  @Pure public abstract double doubleValue(@GuardSatisfied Number this);
  @Pure public byte byteValue(@GuardSatisfied Number this) { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue(@GuardSatisfied Number this) { throw new RuntimeException("skeleton method"); }
}
