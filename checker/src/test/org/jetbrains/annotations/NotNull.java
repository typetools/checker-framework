// Upstream version:
// https://github.com/JetBrains/intellij-community/blob/master/platform/annotations/java8/src/org/jetbrains/annotations/NotNull.java

package org.jetbrains.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.LOCAL_VARIABLE,
    ElementType.TYPE_USE
})
public @interface NotNull {
    String value() default "";
}
