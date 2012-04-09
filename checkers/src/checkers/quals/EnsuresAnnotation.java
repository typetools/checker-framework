package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.flow.util.FlowExpressionParseUtil;

/**
 * A postcondition annotation to indicate that a method ensures that certain
 * expressions have a certain annotation once the method has finished. The
 * expressions for which the annotation must hold after the methods execution
 * are indicated by {@code expression} and are specified using a string. The
 * syntax is specified as part of the documentation of
 * {@link FlowExpressionParseUtil}. The annotation is specified by
 * {@code annotation}.
 * 
 * @author Stefan Heule
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface EnsuresAnnotation {
    String[] expression();

    Class<? extends Annotation> annotation();
}
