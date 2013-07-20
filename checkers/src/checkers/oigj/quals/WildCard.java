package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.PolymorphicQualifier;
import checkers.quals.TypeQualifier;

/**
 * @checker.framework.manual #oigj-checker OIGJ Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@PolymorphicQualifier
public @interface WildCard {}
