package org.eclipse.jgit.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface NonNull {
}
