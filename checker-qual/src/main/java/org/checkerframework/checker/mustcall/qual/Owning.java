package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that ownership should be transferred to the annotated parameter, field, or
 * (when written on a method) return value, for the purposes of Must Call checking. Static fields
 * cannot be owning.
 *
 * <p>This annotation is a declaration annotation rather than a type annotation, because it does not
 * logically belong to any type hierarchy. Logically, it is a directive to the Resource Leak Checker
 * that informs it whether checking that a value's must-call obligations have been satisfied is
 * necessary, or not. In that way, it can be viewed as an annotation expressing an aliasing
 * relationship: passing a object with a non-empty must-call obligation to a method with an owning
 * parameter resolves that object's must-call obligation, because the ownership annotation expresses
 * that the object at the call site and the parameter in the method's body are aliases, and so
 * checking only one of the two is required.
 *
 * <p>Method return values are treated as if they have this annotation by default unless their
 * method is annotated as {@link NotOwning}.
 *
 * <p>When the -AnoLightweightOwnership command-line argument is passed to the checker, this
 * annotation and {@link NotOwning} are ignored.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Owning {}
