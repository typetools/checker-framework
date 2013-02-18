package com.sun.javadoc;

import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;

public interface RootDoc extends Doc, DocErrorReporter {
    String[][] options();
    PackageDoc[] specifiedPackages();
    ClassDoc[] specifiedClasses();
    @NonNull ClassDoc @NonNull [] classes();
    PackageDoc packageNamed(String name);
    ClassDoc classNamed(String qualifiedName);
}
