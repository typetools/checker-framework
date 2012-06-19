package checkers.quals;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import checkers.flow.util.FlowExpressionParseUtil;

/**
 * A conditional postcondition annotation to indicate that a method ensures that
 * certain expressions have a certain annotation once the method has finished,
 * and if the result is as indicated by {@code result}. The expressions for
 * which the annotation must hold after the methods execution are indicated by
 * {@code expression} and are specified using a string. The syntax is specified
 * as part of the documentation of {@link FlowExpressionParseUtil}. The
 * annotation is specified by {@code annotation}.
 * 
 * <p>
 * This annotation is only applicable to methods with a boolean return type.
 * 
 * @author Stefan Heule
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface EnsuresAnnotationIf {
    String[] expression();

    Class<? extends Annotation> annotation();

    boolean result();
}
