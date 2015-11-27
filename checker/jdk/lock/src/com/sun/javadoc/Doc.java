package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public interface Doc extends java.lang.Comparable<java.lang.Object> {
  public abstract java.lang.String commentText();
  public abstract com.sun.javadoc.Tag [] tags();
  public abstract com.sun.javadoc.Tag [] tags(java.lang.String a1);
  public abstract com.sun.javadoc.SeeTag [] seeTags();
  public abstract com.sun.javadoc.Tag [] inlineTags();
  public abstract com.sun.javadoc.Tag [] firstSentenceTags();
  public abstract java.lang.String getRawCommentText();
  public abstract void setRawCommentText(java.lang.String a1);
  public abstract java.lang.String name();
  @Pure public abstract int compareTo(@GuardSatisfied Doc this,java.lang.Object a1);
  @Pure public abstract boolean isField(@GuardSatisfied Doc this);
  @Pure public abstract boolean isEnumConstant(@GuardSatisfied Doc this);
  @Pure public abstract boolean isConstructor(@GuardSatisfied Doc this);
  @Pure public abstract boolean isMethod(@GuardSatisfied Doc this);
  @Pure public abstract boolean isAnnotationTypeElement(@GuardSatisfied Doc this);
  @Pure public abstract boolean isInterface(@GuardSatisfied Doc this);
  @Pure public abstract boolean isException(@GuardSatisfied Doc this);
  @Pure public abstract boolean isError(@GuardSatisfied Doc this);
  @Pure public abstract boolean isEnum(@GuardSatisfied Doc this);
  @Pure public abstract boolean isAnnotationType(@GuardSatisfied Doc this);
  @Pure public abstract boolean isOrdinaryClass(@GuardSatisfied Doc this);
  @Pure public abstract boolean isClass(@GuardSatisfied Doc this);
  @Pure public abstract boolean isIncluded(@GuardSatisfied Doc this);
  public abstract com.sun.javadoc. SourcePosition position();
}
