package java.util;
import checkers.igj.quals.*;

@I
public class BitSet{
  public BitSet() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public BitSet(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void flip(int a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void flip(int a1, int a2) @Mutable { throw new RuntimeException("skeleton method"); }
  public void set(int a1) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void set(int a1, boolean a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void set(int a1, int a2) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void set(int a1, int a2, boolean a3) @AssignsFields { throw new RuntimeException("skeleton method"); }
  public void clear(int a1) @AssignsFields  { throw new RuntimeException("skeleton method"); }
  public void clear(int a1, int a2) @AssignsFields  { throw new RuntimeException("skeleton method"); }
  public void clear() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public boolean get(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public @I("T") java.util.BitSet get(int a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextSetBit(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextClearBit(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int length() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean intersects(@ReadOnly java.util.BitSet a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int cardinality() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void and(@ReadOnly java.util.BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void or(@ReadOnly java.util.BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void xor(@ReadOnly java.util.BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void andNot(@ReadOnly java.util.BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public java.lang.Object clone() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
