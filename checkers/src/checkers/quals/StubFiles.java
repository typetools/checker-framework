package checkers.quals;

import java.lang.annotation.*;

/**
 * An annotation on a SourceChecker subclass to provide additional
 * stub files that should be used in addition to jdk.astub.
 * This allows larger compound checkers to separate the annotations
 * into multiple files.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StubFiles {
    // Stub file names.
    String[] value();
}