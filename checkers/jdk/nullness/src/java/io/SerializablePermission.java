package java.io;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class SerializablePermission extends java.security.BasicPermission {
  private static final long serialVersionUID = 0;
  public SerializablePermission(String a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public SerializablePermission(String a1, @Nullable String a2) { super(a1); throw new RuntimeException("skeleton method"); }
}
