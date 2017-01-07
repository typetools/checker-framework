package java.util;
import org.checkerframework.checker.lock.qual.*;

public final class PropertyPermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 0L;
  public PropertyPermission(String a1, String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied PropertyPermission this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied PropertyPermission this) { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
