package java.util;
import checkers.igj.quals.*;

@Immutable
public final class PropertyPermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 0L;
  public PropertyPermission(@AssignsFields PropertyPermission this, String a1, String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public @I java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
