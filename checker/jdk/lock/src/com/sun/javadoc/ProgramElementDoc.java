package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;

public interface ProgramElementDoc extends Doc {
  public abstract com.sun.javadoc. ClassDoc containingClass();
  public abstract com.sun.javadoc.PackageDoc containingPackage();
  public abstract java.lang.String qualifiedName();
  public abstract int modifierSpecifier();
  public abstract java.lang.String modifiers();
  public abstract com.sun.javadoc.AnnotationDesc[] annotations();
   public abstract boolean isPublic(@GuardSatisfied ProgramElementDoc this);
   public abstract boolean isProtected(@GuardSatisfied ProgramElementDoc this);
   public abstract boolean isPrivate(@GuardSatisfied ProgramElementDoc this);
   public abstract boolean isPackagePrivate(@GuardSatisfied ProgramElementDoc this);
   public abstract boolean isStatic(@GuardSatisfied ProgramElementDoc this);
   public abstract boolean isFinal(@GuardSatisfied ProgramElementDoc this);
}
