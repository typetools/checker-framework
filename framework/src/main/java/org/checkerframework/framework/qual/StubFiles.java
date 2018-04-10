package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a SourceChecker subclass to provide additional stub files that should be used in
 * addition to jdk.astub. This allows larger compound checkers to separate the annotations into
 * multiple files.
 *
 * <p>This annotation is not inherited. That means that if a checker with this annotation is
 * subclassed, then this annotation must be copied to the subclass and the stub file must also be
 * copied to the directory that contains the subclass.
 *
 * @checker_framework.manual #creating-a-checker-annotated-jdk Annotated JDK
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StubFiles {
    // Stub file names.
    String[] value();
}
