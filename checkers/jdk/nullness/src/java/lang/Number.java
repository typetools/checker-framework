package java.lang;

import checkers.nonnull.quals.Nullable;


public abstract class Number implements java.io.Serializable {
  public Number() { throw new RuntimeException("skeleton method"); }
  public abstract int intValue();
  public abstract long longValue();
  public abstract float floatValue();
  public abstract double doubleValue();
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
}
