package com.sun.javadoc;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RootDoc extends Doc, DocErrorReporter {
    String[][] options();
    PackageDoc[] specifiedPackages();
    ClassDoc[] specifiedClasses();
    @NonNull ClassDoc @NonNull [] classes();
    PackageDoc packageNamed(String name);
    ClassDoc classNamed(String qualifiedName);
}
