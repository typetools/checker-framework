package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a field's type, in the class on which this annotation is written, is a subtype of
 * its declared type. The field must be declared in a superclass and must be final.
 *
 * <p>The {@code @FieldInvariant} annotation does not currently accommodate type qualifiers with
 * attributes, such as {@code @MinLen(1)}. In this case, the type system should implement its own
 * field invariant annotation and override {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#getFieldInvariantDeclarationAnnotations()}
 * and {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#getFieldInvariants(javax.lang.model.element.TypeElement)}.
 * See {@link org.checkerframework.common.value.qual.MinLenFieldInvariant} for example.
 *
 * @checker_framework.manual #field-invariants Field invariants
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface FieldInvariant {

    /**
     * The qualifier on the field. Must be a subtype of the qualifier on the declaration of the
     * field.
     */
    Class<? extends Annotation>[] qualifier();

    /**
     * The field that has a more precise type, in the class on which the {@code FieldInvariant}
     * annotation is written. The field must be declared in a superclass and must be {@code final}.
     */
    String[] field();
}
