package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

/**
 * Tainted is the annotation to specify the tainted qualifier.
 *
 * <pre>
 *  &#064;ClassTaintingParam("aParam")
 *  class A {
 *  }
 *
 *  class B {
 *    &#064;Tainted(param="aParam") A f1;
 *    &#064;Tainted(param="aParam", wildcard=Wildcard.EXTENDS) A f2;
 *  }
 *
 *  is equivalent to:
 *  class A&laquo;aParam&raquo; {
 *
 *  }
 *  class B {
 *    A&laquo;aParam=@Tainted&raquo; a;
 *    A&laquo;aParam=? extends @Tainted&raquo; b;
 *  }
 * </pre>
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiTainted.class)
public @interface Tainted {
    /**
     * The name of the parameter to set.
     */
    String param() default SimpleQualifierParameterAnnotationConverter.PRIMARY_TARGET;

    /**
     * Specify that this use is a wildcard with a bound.
     */
    Wildcard wildcard() default Wildcard.NONE;
}
