package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;

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
   public abstract int compareTo(@GuardSatisfied Doc this,java.lang.Object a1);
   public abstract boolean isField(@GuardSatisfied Doc this);
   public abstract boolean isEnumConstant(@GuardSatisfied Doc this);
   public abstract boolean isConstructor(@GuardSatisfied Doc this);
   public abstract boolean isMethod(@GuardSatisfied Doc this);
   public abstract boolean isAnnotationTypeElement(@GuardSatisfied Doc this);
   public abstract boolean isInterface(@GuardSatisfied Doc this);
   public abstract boolean isException(@GuardSatisfied Doc this);
   public abstract boolean isError(@GuardSatisfied Doc this);
   public abstract boolean isEnum(@GuardSatisfied Doc this);
   public abstract boolean isAnnotationType(@GuardSatisfied Doc this);
   public abstract boolean isOrdinaryClass(@GuardSatisfied Doc this);
   public abstract boolean isClass(@GuardSatisfied Doc this);
   public abstract boolean isIncluded(@GuardSatisfied Doc this);
  public abstract com.sun.javadoc. SourcePosition position();
}
