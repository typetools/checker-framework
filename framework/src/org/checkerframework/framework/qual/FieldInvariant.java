package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.element.TypeElement;
/**
 * Specifies that a field's type in the class on which this annotation is written is a subtype of
 * its declared type. The field must be declared in a superclass and must be final.
 *
 * <p>If a type system includes qualifiers with attributes such as {@code @MinLen(1)} then, the type
 * system can implement its own field invariant annotation and override {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#getFieldInvariantDeclarationAnnotations()}
 * and {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#getFieldInvariants(TypeElement)}. See
 * {@link org.checkerframework.checker.index.qual.MinLenFieldInvariant} for example.
 *
 * @checker_framework.manual #field-invariants Field invariants
 */
@Target({ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldInvariant {
    /**
     * The field that has the qualifier. The field must be final and declared in a superclass of the
     * class on which the field invariant applies.
     */
    String[] field();

    /**
     * The qualifier on the field. Must be a subtype of the annotation on the declaration of the
     * field.
     */
    Class<? extends Annotation>[] qualifier();
}
