package java.util;
import checkers.igj.quals.*;

@Immutable
public final class PropertyPermission extends java.security.BasicPermission {
  public PropertyPermission(java.lang.String a1, java.lang.String a2) @AssignsFields { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@ReadOnly java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public java.lang.String getActions() { throw new RuntimeException("skeleton method"); }
  public @I java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
