package java.util;
import checkers.igj.quals.*;

@I
public class BitSet implements @I Cloneable, @I java.io.Serializable {
    private static final long serialVersionUID = 0L;
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
  public @I("T") BitSet get(int a1, int a2) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextSetBit(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int nextClearBit(int a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int length() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean intersects(@ReadOnly BitSet a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int cardinality() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void and(@ReadOnly BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void or(@ReadOnly BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void xor(@ReadOnly BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void andNot(@ReadOnly BitSet a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public int hashCode() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public int size() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public String toString() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public Object clone() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
