package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class FilePermission extends java.security.Permission implements Serializable {
  private static final long serialVersionUID = 0;
  public FilePermission(String a1, String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(@Nullable java.security.Permission a1) { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
