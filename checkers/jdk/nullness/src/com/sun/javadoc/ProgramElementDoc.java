package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface ProgramElementDoc extends Doc {
  public abstract @Nullable com.sun.javadoc.ClassDoc containingClass();
  public abstract @NonNull com.sun.javadoc.PackageDoc containingPackage();
  public abstract @NonNull java.lang.String qualifiedName();
  public abstract int modifierSpecifier();
  public abstract @NonNull java.lang.String modifiers();
  public abstract @NonNull com.sun.javadoc.AnnotationDesc @NonNull [] annotations();
  public abstract boolean isPublic();
  public abstract boolean isProtected();
  public abstract boolean isPrivate();
  public abstract boolean isPackagePrivate();
  public abstract boolean isStatic();
  public abstract boolean isFinal();
}
