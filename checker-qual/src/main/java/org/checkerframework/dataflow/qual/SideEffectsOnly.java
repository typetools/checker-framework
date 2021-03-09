package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * A method annotated with the declaration annotation {@code @SideEffectsOnly} could side effect
 * those expressions that are supplied as annotation values to {@code @SideEffectsOnly}. The
 * annotation values are an upper bound of all expressions that the method side-effects.
 *
 * @checker_framework.manual #type-refinement-purity Side effects only
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectsOnly {
    /**
     * An upper bound of the expressions that this method side effects.
     *
     * @return Java expression(s) that represent an upper bound of expressions side-effected by this
     *     method
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    @JavaExpression
    public String[] value();
}
