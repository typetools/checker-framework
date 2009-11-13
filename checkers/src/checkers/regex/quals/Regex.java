package checkers.regex.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Indicates that a {@code String} is a valid regular expression.
 */
@Documented
@TypeQualifier
@Inherited
@SubtypeOf({Unqualified.class})
@Retention(RetentionPolicy.RUNTIME)
public @interface Regex {}
