package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class RuntimePermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 0L;
  public RuntimePermission(String a1) { super(a1); throw new RuntimeException("skeleton method"); }
  public RuntimePermission(String a1, @Nullable String a2) { super(a1); throw new RuntimeException("skeleton method"); }
}
