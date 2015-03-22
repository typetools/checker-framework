package org.netbeans.api.annotations.common;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@TypeQualifier
public @interface CheckForNull {
}
