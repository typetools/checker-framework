package checkers.regex;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Indicates that a {@code String} is a valid Regular Expression String.
 *
 */
@Documented
@TypeQualifier
@Inherited
@SubtypeOf({Unqualified.class})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRegex {}
