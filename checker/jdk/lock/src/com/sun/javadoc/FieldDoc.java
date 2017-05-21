package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;

public interface FieldDoc extends MemberDoc {
  public abstract com.sun.javadoc.Type type();
   public abstract boolean isTransient(@GuardSatisfied FieldDoc this);
   public abstract boolean isVolatile(@GuardSatisfied FieldDoc this);
  public abstract com.sun.javadoc.SerialFieldTag[] serialFieldTags();
  public abstract Object constantValue();
  public abstract java.lang. String constantValueExpression();
}
