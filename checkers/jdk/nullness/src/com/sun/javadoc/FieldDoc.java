package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface FieldDoc extends MemberDoc {
  public abstract @NonNull com.sun.javadoc.Type type();
  public abstract boolean isTransient();
  public abstract boolean isVolatile();
  public abstract @NonNull com.sun.javadoc.SerialFieldTag @NonNull [] serialFieldTags();
  public abstract @Nullable Object constantValue();
  public abstract @Nullable java.lang.String constantValueExpression();
}
