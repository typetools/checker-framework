package java.io;

import org.checkerframework.checker.lock.qual.*;



public final class FilePermission extends java.security.Permission implements Serializable {
  private static final long serialVersionUID = 0;
  public FilePermission(String a1, String a2) { super(a1); throw new RuntimeException("skeleton method"); }
  public boolean implies(java.security. Permission a1) { throw new RuntimeException("skeleton method"); }
   public boolean equals(@GuardSatisfied FilePermission this,@GuardSatisfied Object a1) { throw new RuntimeException("skeleton method"); }
   public int hashCode(@GuardSatisfied FilePermission this) { throw new RuntimeException("skeleton method"); }
  public String getActions() { throw new RuntimeException("skeleton method"); }
  public java.security.PermissionCollection newPermissionCollection() { throw new RuntimeException("skeleton method"); }
}
