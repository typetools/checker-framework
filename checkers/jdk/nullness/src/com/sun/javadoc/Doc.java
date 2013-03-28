package com.sun.javadoc;

import checkers.nullness.quals.Nullable;

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
  public abstract int compareTo(java.lang.Object a1);
  public abstract boolean isField();
  public abstract boolean isEnumConstant();
  public abstract boolean isConstructor();
  public abstract boolean isMethod();
  public abstract boolean isAnnotationTypeElement();
  public abstract boolean isInterface();
  public abstract boolean isException();
  public abstract boolean isError();
  public abstract boolean isEnum();
  public abstract boolean isAnnotationType();
  public abstract boolean isOrdinaryClass();
  public abstract boolean isClass();
  public abstract boolean isIncluded();
  public abstract com.sun.javadoc. @Nullable SourcePosition position();
}
