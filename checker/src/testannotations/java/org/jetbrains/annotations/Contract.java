// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/JetBrains/java-annotations/blob/master/src/jvmMain/java/org/jetbrains/annotations/Contract.java

package org.jetbrains.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Contract {
  String value() default "";

  boolean pure() default false;

  String mutates() default "";
}
