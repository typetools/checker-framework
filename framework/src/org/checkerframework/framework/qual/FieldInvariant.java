package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.lang.model.element.TypeElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
/**
 * Specifies that a field's type in the class on which this annotation is written is a subtype of
 * its declared type. The field must be declared in a superclass and must be final. For example,
 *
 * <pre><code>
 * class Super {
 *   final @Nullable Object field;
 *   public Super(@Nullable Object field) {
 *     this.field = field;
 *   }
 * }
 *
 * {@code @FieldInvariant(qualifier = NonNull.class, field = "field")}
 * class Sub {
 *   public Sub(Object field) {
 *     super(field);
 *   }
 *   void method() {
 *     field.toString() // legal, field is non-null in this class.
 *   }
 * }
 * </code></pre>
 *
 * A field invariant annotation can refer to more than one field. For example,
 * {@code @FieldInvariant(qualifier = NonNull.class, field = {fieldA, fieldB})} means that {@code
 * fieldA} and {@code fieldB} are both non-null. A field invariant annotation can also apply
 * different qualifiers to fields. For example, {@code @FieldInvariant(qualifier = {NonNull.class,
 * Untainted.class}, field = {fieldA, fieldB})} means that {@code fieldA} is non-null and {@code
 * fieldB} is untainted.
 *
 * <p>This annotation is inherited so, if a superclass is annotated with @FieldInvariant, its
 * subclasses have the same annotation. If a subclass has its own @FieldInvariant, then it must
 * include the fields in the superclass annotation and those fields' annotations must be a subtype
 * (or equal) to the annotations for those fields in the the superclass @FieldInvariant.
 *
 * <p>If a type system includes qualifiers with attributes such as {@code @MinLen(1)} then, the type
 * system can implement its own field invariant annotation and override {@link
 * AnnotatedTypeFactory#getFieldInvariantDeclarationAnnotations()} and {@link
 * AnnotatedTypeFactory#getFieldInvariants(TypeElement)}. See {@link
 * org.checkerframework.checker.index.qual.MinLenFieldInvariant} for example.
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
