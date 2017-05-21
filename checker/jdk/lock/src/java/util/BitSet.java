package java.util;

import org.checkerframework.checker.lock.qual.*;

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
   public boolean get(@GuardSatisfied BitSet this,int a1) { throw new RuntimeException("skeleton method"); }
   public BitSet get(@GuardSatisfied BitSet this,int a1, int a2) { throw new RuntimeException("skeleton method"); }
   public int nextSetBit(@GuardSatisfied BitSet this,int a1) { throw new RuntimeException("skeleton method"); }
   public int nextClearBit(@GuardSatisfied BitSet this,int a1) { throw new RuntimeException("skeleton method"); }
   public int length(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
   public boolean isEmpty(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
   public boolean intersects(@GuardSatisfied BitSet this,@GuardSatisfied BitSet a1) { throw new RuntimeException("skeleton method"); }
   public int cardinality(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
  public void and(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void or(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void xor(BitSet a1) { throw new RuntimeException("skeleton method"); }
  public void andNot(BitSet a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
   public int size(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied BitSet this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public String toString(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
   public Object clone(@GuardSatisfied BitSet this) { throw new RuntimeException("skeleton method"); }
}
