package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declaration annotation applied to type declarations to specify that the upper bound qualifier
 * should not be applied to unannotated uses of the type.
 */
@Target(ElementType.TYPE)
public @interface NoDefaultQualifierForUse {
    /** Top qualifier in hierarchies for which no default annotation for use should be appied. */
    Class<? extends Annotation>[] value() default {};
}
