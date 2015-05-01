package org.checkerframework.checker.oigj.qual;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 *
 * Indicates the annotated {@code Field} may be re-assigned regardless of the
 * immutability of the enclosing class or object instance.
 *
 * @checker_framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { FIELD } )
public @interface Assignable {

}
