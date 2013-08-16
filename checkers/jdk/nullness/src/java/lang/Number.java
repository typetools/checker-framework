package java.lang;

import dataflow.quals.Pure;
import checkers.nullness.quals.Nullable;


public abstract class Number implements java.io.Serializable {
  public Number() { throw new RuntimeException("skeleton method"); }
    private static final long serialVersionUID = -8742448824652078965L;
  @Pure public abstract int intValue();
  @Pure public abstract long longValue();
  @Pure public abstract float floatValue();
  @Pure public abstract double doubleValue();
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
}
