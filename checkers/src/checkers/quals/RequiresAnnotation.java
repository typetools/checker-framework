package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.flow.util.FlowExpressionParseUtil;

/**
 * A precondition annotation to indicate that a method requires certain
 * expressions to have a certain annotation at the time of the call to the
 * method. The expressions for which the annotation must hold after the methods
 * execution are indicated by {@code expression} and are specified using a
 * string. The syntax is specified as part of the documentation of
 * {@link FlowExpressionParseUtil}. The annotation is specified by
 * {@code annotation}.
 * 
 * @author Stefan Heule
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface RequiresAnnotation {
    String[] expression();

    Class<? extends Annotation> annotation();
}
