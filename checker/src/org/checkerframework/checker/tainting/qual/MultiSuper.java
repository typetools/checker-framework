package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.framework.qual.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MultiSuper {
    Super[] value();
}
