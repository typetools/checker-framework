package com.sun.javadoc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public interface FieldDoc extends MemberDoc {
  public abstract com.sun.javadoc.Type type();
  @Pure public abstract boolean isTransient();
  @Pure public abstract boolean isVolatile();
  public abstract com.sun.javadoc.SerialFieldTag[] serialFieldTags();
  public abstract @Nullable Object constantValue();
  public abstract java.lang. @Nullable String constantValueExpression();
}
