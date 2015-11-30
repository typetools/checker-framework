package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;
import org.checkerframework.dataflow.qual.Pure;

public interface ProgramElementDoc extends Doc {
  public abstract com.sun.javadoc. ClassDoc containingClass();
  public abstract com.sun.javadoc.PackageDoc containingPackage();
  public abstract java.lang.String qualifiedName();
  public abstract int modifierSpecifier();
  public abstract java.lang.String modifiers();
  public abstract com.sun.javadoc.AnnotationDesc[] annotations();
  @Pure public abstract boolean isPublic(@GuardSatisfied ProgramElementDoc this);
  @Pure public abstract boolean isProtected(@GuardSatisfied ProgramElementDoc this);
  @Pure public abstract boolean isPrivate(@GuardSatisfied ProgramElementDoc this);
  @Pure public abstract boolean isPackagePrivate(@GuardSatisfied ProgramElementDoc this);
  @Pure public abstract boolean isStatic(@GuardSatisfied ProgramElementDoc this);
  @Pure public abstract boolean isFinal(@GuardSatisfied ProgramElementDoc this);
}
