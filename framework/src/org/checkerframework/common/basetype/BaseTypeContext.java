package org.checkerframework.common.basetype;

import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.CFContext;

/** An extension of {@link CFContext} that includes {@link
 * BaseTypeChecker}-specific components. */
public interface BaseTypeContext extends CFContext {
    // TODO: Using covariant return types in interfaces under `javac -source
    // 1.8` (the default) generates bridge methods in the interface itself
    // (instead of generating them only in implementing classes), using Java
    // 8's new "default method" functionality.  The generated class files are
    // thus incompatible with the Java 7 runtime.  Compiling with `javac
    // -source 1.7` doesn't generate these default methods, but I'm not sure
    // how to modify the build.xml to make that happen.

    //BaseTypeChecker getChecker();
    //BaseTypeVisitor<?> getVisitor();

    GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory();
}
