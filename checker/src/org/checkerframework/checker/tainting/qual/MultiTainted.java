package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MultiTainted {
    Tainted[] value();
}
