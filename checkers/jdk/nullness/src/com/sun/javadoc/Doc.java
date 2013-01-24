package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface Doc extends java.lang.Comparable<java.lang.Object> {
  public abstract java.lang. @NonNull String commentText();
  public abstract com.sun.javadoc. @NonNull Tag @NonNull [] tags();
  public abstract com.sun.javadoc. @NonNull Tag @NonNull [] tags(java.lang.String a1);
  public abstract com.sun.javadoc. @NonNull SeeTag @NonNull [] seeTags();
  public abstract com.sun.javadoc. @NonNull Tag @NonNull [] inlineTags();
  public abstract com.sun.javadoc. @NonNull Tag @NonNull [] firstSentenceTags();
  public abstract java.lang. @NonNull String getRawCommentText();
  public abstract void setRawCommentText(java.lang. @NonNull String a1);
  public abstract java.lang. @NonNull String name();
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
