package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier specifies that
 * the given annotation should be the default for a particular location,
 * only when the symbol is from unannotated code (bytecode or source code).
 * Examples of unannotated code are: bytecode imported from a jar file that
 * does not have a stub file describing it, or partially annotated
 * library source code that is not annotated with {code @AnnotatedFor}
 * for the checker being executed.
 * <p>
 *
 * This qualifier will only apply to unannotated bytecode if the
 * -AsafeDefaultsForUncheckedBytecode command-line option is passed.
 * It will only apply to unannotated source code if the
 * -AuseConservativeDefaultsForUnannotatedSourceCode command-line
 * option is passed and the source code is not annotated with
 * {code @AnnotatedFor} for the checker being executed.
 * <p>
 *
 * Note, this annotation is analogous to {@code DefaultFor} but for
 * unannotated code.
 * This qualifier is for type system developers, not end-users.
 *
 * @see AnnotatedFor
 * @see DefaultQualifier
 * @see DefaultQualifierForUnannotatedCode
 * @see ImplicitFor
 * @checker_framework.manual #defaults-classfile Default qualifiers for .class files (conservative library defaults)
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultForUnannotatedCode {
    /**
     * @return the locations to which the annotation should be applied
     */
    DefaultLocation[] value();
}
