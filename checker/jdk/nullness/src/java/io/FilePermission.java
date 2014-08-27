package java.io;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.nullness.qual.Nullable;

@org.checkerframework.framework.qual.DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)

public final class FilePermission extends java.security.Permission implements Serializable {
  private static final long serialVersionUID = 0;
  public FilePermission(String a1, String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security. @Nullable Permission a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
