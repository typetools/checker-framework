package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import java.lang.annotation.*;

/**
 * Indicates that the annotated qualifier is the default qualifier on types
 * from unannotated code (bytecode or source code).  Examples of unannotated code are:
 * bytecode imported from a jar file that does not have a stub file describing it,
 * or partially annotated library source code that is not annotated with {code @AnnotatedFor}
 * for the checker being executed.
 * <p>
 *
 * This qualifier will only apply to unannotated bytecode if the
 * -AsafeDefaultsForUnannotatedBytecode command-line option is passed.
 * It will only apply to unannotated source code if the
 * -AuseConservativeDefaultsForUnannotatedSourceCode command-line
 * option is passed and the source code is not annotated with
 * {code @AnnotatedFor} for the checker being executed.
 * <p>
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * {@code DefaultQualifierForUnannotatedCode}.
 * <p>
 *
 * Note, this annotation is analogous to {@code DefaultQualifierInHierarchy} but for
 * unannotated code.
 * This qualifier is for type system developers, not end-users.
 * @see AnnotatedFor
 * @see DefaultForUnannotatedCode
 * @checker_framework.manual #defaults-classfile Default qualifiers for .class files (conservative library defaults)
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface DefaultQualifierForUnannotatedCode {}
