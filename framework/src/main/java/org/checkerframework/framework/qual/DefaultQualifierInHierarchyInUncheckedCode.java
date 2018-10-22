package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated qualifier is the default qualifier on unannotated type uses (in
 * bytecode or source code that has not been type-checked).
 *
 * <p>This qualifier only applies if the {@code -AuseDefaultsForUncheckedCode} command-line option
 * enables unchecked code defaults. They can be enabled for source and bytecode separately. If the
 * unchecked code defaults are enabled for source code, they will only be applied to source code not
 * annotated with {@link AnnotatedFor} for the checker being executed.
 *
 * <p>Each type qualifier hierarchy may have at most one qualifier marked as {@code
 * DefaultQualifierInHierarchyInUncheckedCode}.
 *
 * <p>Note, this annotation is analogous to {@code @}{@link DefaultQualifierInHierarchy} but for
 * unannotated type uses in code that has not been type-checked.
 *
 * <p>If a checker does not specify a default qualifier for unchecked code, then the defaults for
 * checked code will be used.
 *
 * <p>This qualifier is for type system developers, not end-users.
 *
 * @see AnnotatedFor
 * @see DefaultInUncheckedCodeFor
 * @checker_framework.manual #defaults-classfile Default qualifiers for .class files (conservative
 *     library defaults)
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultQualifierInHierarchyInUncheckedCode {}
