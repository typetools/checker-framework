package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier specifies that
 * the given annotation should be the default for unannotated type uses
 * (in bytecode or source code) at the given location(s).
 * <p>
 *
 * This qualifier will only apply to unannotated type uses in bytecode if the
 * -AsafeDefaultsForUnannotatedBytecode command-line option is passed.
 * It will only apply to unannotated type uses in source code if the
 * -AuseSafeDefaultsForUnannotatedSourceCode command-line
 * option is passed and the source code is not annotated with
 * {@link AnnotatedFor} for the checker being executed.
 * <p>
 *
 * Note, this annotation is analogous to {@link DefaultFor} but for
 * unannotated type uses.
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
