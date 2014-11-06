package com.sun.javadoc;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public abstract interface ProgramElementDoc extends Doc {
  public abstract com.sun.javadoc. @Nullable ClassDoc containingClass();
  public abstract com.sun.javadoc.PackageDoc containingPackage();
  public abstract java.lang.String qualifiedName();
  public abstract int modifierSpecifier();
  public abstract java.lang.String modifiers();
  public abstract com.sun.javadoc.AnnotationDesc[] annotations();
  @Pure public abstract boolean isPublic();
  @Pure public abstract boolean isProtected();
  @Pure public abstract boolean isPrivate();
  @Pure public abstract boolean isPackagePrivate();
  @Pure public abstract boolean isStatic();
  @Pure public abstract boolean isFinal();
}
