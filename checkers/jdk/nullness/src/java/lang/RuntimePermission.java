package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class RuntimePermission extends java.security.BasicPermission {
  public RuntimePermission(java.lang.String a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public RuntimePermission(java.lang.String a1, @Nullable java.lang.String a2) { super(a1); throw new RuntimeException("skeleton method"); }
}
