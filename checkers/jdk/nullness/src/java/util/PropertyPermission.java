package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class PropertyPermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 0L;
  public PropertyPermission(String a1, @Nullable String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
