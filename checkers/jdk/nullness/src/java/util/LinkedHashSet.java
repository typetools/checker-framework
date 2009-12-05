package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

// This class permits null elements
public class LinkedHashSet<E extends @Nullable Object> extends java.util.HashSet<E> implements java.util.Set<E>, java.lang.Cloneable, java.io.Serializable {
  private static final long serialVersionUID = 0;
  public LinkedHashSet(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public LinkedHashSet(int a1) { throw new RuntimeException("skeleton method"); }
  public LinkedHashSet() { throw new RuntimeException("skeleton method"); }
  public LinkedHashSet(java.util.Collection<? extends E> a1) { throw new RuntimeException("skeleton method"); }
}
