package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class UUID implements java.io.Serializable, Comparable<UUID> {
    private static final long serialVersionUID = 0L;
  public UUID(long a1, long a2) { throw new RuntimeException("skeleton method"); }
  public static UUID randomUUID() { throw new RuntimeException("skeleton method"); }
  public static UUID nameUUIDFromBytes(byte[] a1) { throw new RuntimeException("skeleton method"); }
  public static UUID fromString(String a1) { throw new RuntimeException("skeleton method"); }
  public long getLeastSignificantBits() { throw new RuntimeException("skeleton method"); }
  public long getMostSignificantBits() { throw new RuntimeException("skeleton method"); }
  public int version() { throw new RuntimeException("skeleton method"); }
  public int variant() { throw new RuntimeException("skeleton method"); }
  public long timestamp() { throw new RuntimeException("skeleton method"); }
  public int clockSequence() { throw new RuntimeException("skeleton method"); }
  public long node() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(UUID a1) { throw new RuntimeException("skeleton method"); }
}
