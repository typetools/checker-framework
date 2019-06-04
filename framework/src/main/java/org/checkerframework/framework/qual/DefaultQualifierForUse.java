package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface DefaultQualifierForUse {

    /** Qualifier to add to all unannotated uses of the type with this declaration annotation. */
    Class<? extends Annotation>[] value() default {};

    /**
     * Whether any qualifier should be added to all unannotated uses with this declaration
     * annotation.
     */
    boolean none() default false;
}
