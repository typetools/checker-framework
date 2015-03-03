package org.checkerframework.checker.tainting.qual;

import org.checkerframework.qualframework.poly.SimpleQualifierParameterAnnotationConverter;
import org.checkerframework.qualframework.poly.qual.Wildcard;

import java.lang.annotation.*;

/**
 * Tainted is the annotation to specify the tainted qualifier.
 *
 * <pre>
 * {@code
 *  {@literal @}TaintingParam("aParam")
 *  class A {
 *  }
 *
 *  class B {
 *    {@literal @}Tainted(param="aParam") A f1;
 *    {@literal @}Tainted(param="aParam", wildcard=Wildcard.EXTENDS) A f2;
 *  }
 *
 *  is equivalent to:
 *  class A《aParam》 {
 *
 *  }
 *  class B {
 *    A《aParam={@literal @}Tainted》 a;
 *    A《aParam=? extends {@literal @}Tainted》 b;
 *  }
 * }
 * </pre>
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
