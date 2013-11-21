package checkers.source;

import java.lang.annotation.*;
import javax.annotation.processing.SupportedOptions;

/**
 * An annotation used to indicate what lint options a checker supports. The
 * {@link SourceChecker#getSupportedLintOptions} method can construct its
 * result from the value of this annotation.
 *
 * @see SupportedOptions
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedLintOptions {
    String[] value();
}
