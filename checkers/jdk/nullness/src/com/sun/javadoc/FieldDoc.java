package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface FieldDoc extends MemberDoc {
  public abstract com.sun.javadoc. @NonNull Type type();
  public abstract boolean isTransient();
  public abstract boolean isVolatile();
  public abstract com.sun.javadoc. @NonNull SerialFieldTag @NonNull [] serialFieldTags();
  public abstract @Nullable Object constantValue();
  public abstract java.lang. @Nullable String constantValueExpression();
}
