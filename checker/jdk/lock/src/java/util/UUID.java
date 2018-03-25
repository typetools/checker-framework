package java.util;
import org.checkerframework.checker.lock.qual.*;

public final class UUID implements java.io.Serializable, Comparable<UUID> {
    private static final long serialVersionUID = 0L;
  public UUID(long a1, long a2) { throw new RuntimeException("skeleton method"); }
  public static UUID randomUUID() { throw new RuntimeException("skeleton method"); }
  public static UUID nameUUIDFromBytes(byte[] a1) { throw new RuntimeException("skeleton method"); }
  public static UUID fromString(String a1) { throw new RuntimeException("skeleton method"); }
  public long getLeastSignificantBits(@GuardSatisfied UUID this) { throw new RuntimeException("skeleton method"); }
  public long getMostSignificantBits(@GuardSatisfied UUID this) { throw new RuntimeException("skeleton method"); }
  public int version() { throw new RuntimeException("skeleton method"); }
  public int variant() { throw new RuntimeException("skeleton method"); }
  public long timestamp() { throw new RuntimeException("skeleton method"); }
  public int clockSequence() { throw new RuntimeException("skeleton method"); }
  public long node() { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied UUID this) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied UUID this) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@GuardSatisfied UUID this, @GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(@GuardSatisfied UUID this, @GuardSatisfied UUID a1) { throw new RuntimeException("skeleton method"); }
}
