package checkers.fenum.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * @author wmdietl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@SubtypeOf(FenumTop.class)
public @interface SwingBoxOrientation {}
