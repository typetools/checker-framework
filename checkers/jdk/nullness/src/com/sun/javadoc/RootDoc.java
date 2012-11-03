package com.sun.javadoc;

import checkers.nullness.quals.*;

public interface RootDoc extends Doc, DocErrorReporter {
    String[][] options();
    PackageDoc[] specifiedPackages();
    ClassDoc[] specifiedClasses();
    @NonNull ClassDoc @NonNull [] classes();
    PackageDoc packageNamed(String name);
    ClassDoc classNamed(String qualifiedName);
}
