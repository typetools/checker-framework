package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * This method declaration annotation can be used to specify the expressions that a method side
 * effects. In other words, the method only side effects those expressions that are supplied as
 * annotation values to {@code @SideEffectsOnly}.
 *
 * @checker_framework.manual #type-refinement-purity Side effects only
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectsOnly {
    /**
     * The expressions that this method side effects.
     *
     * @return Java expressions that are side-effected by this method
     */
    @JavaExpression
    public String[] value();
}
