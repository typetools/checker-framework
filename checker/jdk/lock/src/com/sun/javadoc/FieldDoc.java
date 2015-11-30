package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public interface FieldDoc extends MemberDoc {
  public abstract com.sun.javadoc.Type type();
  @Pure public abstract boolean isTransient(@GuardSatisfied FieldDoc this);
  @Pure public abstract boolean isVolatile(@GuardSatisfied FieldDoc this);
  public abstract com.sun.javadoc.SerialFieldTag[] serialFieldTags();
  public abstract Object constantValue();
  public abstract java.lang. String constantValueExpression();
}
