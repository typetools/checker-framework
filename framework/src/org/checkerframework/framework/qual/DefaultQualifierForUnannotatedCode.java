package org.checkerframework.framework.qual;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;

import java.lang.annotation.*;

/**
 * Indicates that the annotated qualifier is the default qualifier on
 * unannotated type uses (in bytecode or source code).
 * <p>
 *
 * This qualifier will only apply to unannotated type uses in bytecode if the
 * -AsafeDefaultsForUnannotatedBytecode command-line option is passed.
 * It will only apply to unannotated type uses in source code if the
 * -AuseConservativeDefaultsForUnannotatedSourceCode command-line
 * option is passed and the source code is not annotated with
 * {@link AnnotatedFor} for the checker being executed.
 * <p>
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * {@code DefaultQualifierForUnannotatedCode}.
 * <p>
 *
 * Note, this annotation is analogous to {@link DefaultQualifierInHierarchy} but for
 * unannotated type uses.
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
