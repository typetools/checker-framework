package java.util;
import checkers.igj.quals.*;

@Immutable
public final class UUID implements java.io.Serializable, java.lang.Comparable<java.util.UUID> {
    private static final long serialVersionUID = 0L;
  public UUID(long a1, long a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public static java.util.UUID randomUUID() { throw new RuntimeException("skeleton method"); }
  public static java.util.UUID nameUUIDFromBytes(byte @ReadOnly [] a1) { throw new RuntimeException("skeleton method"); }
  public static java.util.UUID fromString(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public long getLeastSignificantBits() { throw new RuntimeException("skeleton method"); }
  public long getMostSignificantBits() { throw new RuntimeException("skeleton method"); }
  public int version() { throw new RuntimeException("skeleton method"); }
  public int variant() { throw new RuntimeException("skeleton method"); }
  public long timestamp() { throw new RuntimeException("skeleton method"); }
  public int clockSequence() { throw new RuntimeException("skeleton method"); }
  public long node() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(@ReadOnly java.util.UUID a1) { throw new RuntimeException("skeleton method"); }
}
