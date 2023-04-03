package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that ownership should be transferred to the annotated element for the
 * purposes of Must Call checking. When written on a parameter, the annotation indicates that Must
 * Call checking should be performed in the body of the method, not at call sites. When written on a
 * method, the annotation indicates that return expressions do not need to be checked in the method
 * body, but at call sites. When written on a field, the annotation indicates that fulfilling the
 * must-call obligations of an instance of the class in which the field is declared also results in
 * the annotated field's must-call obligations being satisfied. Static fields cannot be owning.
 *
 * <p>This annotation is a declaration annotation rather than a type annotation, because it does not
 * logically belong to any type hierarchy. Logically, it is a directive to the Resource Leak Checker
 * that informs it whether it is necessary to check that a value's must-call obligations have been
 * satisfied. In that way, it can be viewed as an annotation expressing an aliasing relationship:
 * passing a object with a non-empty must-call obligation to a method with an owning parameter
 * resolves that object's must-call obligation, because the ownership annotation expresses that the
 * object at the call site and the parameter in the method's body are aliases, and so checking only
 * one of the two is required.
 *
 * <p>Constructor results are always {@code @Owning}. Method return types are {@code @Owning} by
 * default. Formal parameters and fields are {@link NotOwning} by default.
 *
 * <p>When the {@code -AnoLightweightOwnership} command-line argument is passed to the checker, this
 * annotation and {@link NotOwning} are ignored.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Owning {}
