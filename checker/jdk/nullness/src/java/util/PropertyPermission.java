package java.util;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PropertyPermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 0L;
  public PropertyPermission(String a1, @Nullable String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
