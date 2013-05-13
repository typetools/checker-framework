package com.sun.javadoc;

import checkers.nullness.quals.Nullable;

public abstract interface FieldDoc extends MemberDoc {
  public abstract com.sun.javadoc.Type type();
  @Pure public abstract boolean isTransient();
  @Pure public abstract boolean isVolatile();
  public abstract com.sun.javadoc.SerialFieldTag[] serialFieldTags();
  public abstract @Nullable Object constantValue();
  public abstract java.lang. @Nullable String constantValueExpression();
}
