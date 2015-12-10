package org.eclipse.jgit.annotations;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.TypeQualifier;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE_USE})
@TypeQualifier
public @interface NonNull {
}
