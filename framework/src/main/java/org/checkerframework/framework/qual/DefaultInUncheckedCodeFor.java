package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to the declaration of a type qualifier, specifies that the given annotation should be the
 * default for unannotated type uses (in bytecode or source code) at the given location(s).
 *
 * <p>Unchecked code defaults are only applied if they are enabled via the {@code
 * -AuseDefaultsForUncheckedCode} command-line option. They can be enabled for source and bytecode
 * separately. If the unchecked code defaults are enabled for source code, they will only be applied
 * to source code not annotated with {@link AnnotatedFor} for the checker being executed.
 *
 * <p>Note, this annotation is analogous to {@link DefaultFor} but for unannotated type uses in code
 * that has not been type-checked. This qualifier is for type system developers, not end-users.
 *
 * @see AnnotatedFor
 * @see DefaultQualifier
 * @see DefaultQualifierInHierarchyInUncheckedCode
 * @checker_framework.manual #defaults-classfile Default qualifiers for .class files (conservative
 *     library defaults)
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultInUncheckedCodeFor {
    /** @return the locations to which the annotation should be applied */
    TypeUseLocation[] value();
}
