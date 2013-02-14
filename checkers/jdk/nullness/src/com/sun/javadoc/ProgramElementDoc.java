package com.sun.javadoc;

import checkers.nullness.quals.*;

public abstract interface ProgramElementDoc extends Doc {
  public abstract com.sun.javadoc. @Nullable ClassDoc containingClass();
  public abstract com.sun.javadoc. @NonNull PackageDoc containingPackage();
  public abstract java.lang. @NonNull String qualifiedName();
  public abstract int modifierSpecifier();
  public abstract java.lang. @NonNull String modifiers();
  public abstract com.sun.javadoc. @NonNull AnnotationDesc @NonNull [] annotations();
  public abstract boolean isPublic();
  public abstract boolean isProtected();
  public abstract boolean isPrivate();
  public abstract boolean isPackagePrivate();
  public abstract boolean isStatic();
  public abstract boolean isFinal();
}
