package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import org.checkerframework.checker.nullness.qual.Nullable;

public class BitSet implements Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 0L;
  public BitSet() { throw new RuntimeException("skeleton method"); }
  public BitSet(int a1) { throw new RuntimeException("skeleton method"); }
  public void flip(int a1) { throw new RuntimeException("skeleton method"); }
  public void flip(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void set(int a1) { throw new RuntimeException("skeleton method"); }
  public void set(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public void set(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void set(int a1, int a2, boolean a3) { throw new RuntimeException("skeleton method"); }
  public void clear(int a1) { throw new RuntimeException("skeleton method"); }
  public void clear(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void clear() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean get(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public BitSet get(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int nextSetBit(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int nextClearBit(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int length() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean intersects(BitSet a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int cardinality() { throw new RuntimeException("skeleton method"); }
  public void and(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void or(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void xor(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void andNot(BitSet a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public int size() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Object clone() { throw new RuntimeException("skeleton method"); }
}
