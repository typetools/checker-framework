package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface Doc extends java.lang.Comparable<java.lang.Object> {
  public abstract @NonNull java.lang.String commentText();
  public abstract @NonNull com.sun.javadoc.Tag @NonNull [] tags();
  public abstract @NonNull com.sun.javadoc.Tag @NonNull [] tags(java.lang.String a1);
  public abstract @NonNull com.sun.javadoc.SeeTag @NonNull [] seeTags();
  public abstract @NonNull com.sun.javadoc.Tag @NonNull [] inlineTags();
  public abstract @NonNull com.sun.javadoc.Tag @NonNull [] firstSentenceTags();
  public abstract @NonNull java.lang.String getRawCommentText();
  public abstract void setRawCommentText(@NonNull java.lang.String a1);
  public abstract @NonNull java.lang.String name();
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
  public abstract @Nullable com.sun.javadoc.SourcePosition position();
}
