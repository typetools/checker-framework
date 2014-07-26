package com.sun.javadoc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public abstract interface Doc extends java.lang.Comparable<java.lang.Object> {
  public abstract java.lang.String commentText();
  public abstract com.sun.javadoc.Tag [] tags();
  public abstract com.sun.javadoc.Tag [] tags(java.lang.String a1);
  public abstract com.sun.javadoc.SeeTag [] seeTags();
  public abstract com.sun.javadoc.Tag [] inlineTags();
  public abstract com.sun.javadoc.Tag [] firstSentenceTags();
  public abstract java.lang.String getRawCommentText();
  public abstract void setRawCommentText(java.lang.String a1);
  public abstract java.lang.String name();
  @Pure public abstract int compareTo(java.lang.Object a1);
  @Pure public abstract boolean isField();
  @Pure public abstract boolean isEnumConstant();
  @Pure public abstract boolean isConstructor();
  @Pure public abstract boolean isMethod();
  @Pure public abstract boolean isAnnotationTypeElement();
  @Pure public abstract boolean isInterface();
  @Pure public abstract boolean isException();
  @Pure public abstract boolean isError();
  @Pure public abstract boolean isEnum();
  @Pure public abstract boolean isAnnotationType();
  @Pure public abstract boolean isOrdinaryClass();
  @Pure public abstract boolean isClass();
  @Pure public abstract boolean isIncluded();
  public abstract com.sun.javadoc. @Nullable SourcePosition position();
}
