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
//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Regex {}
