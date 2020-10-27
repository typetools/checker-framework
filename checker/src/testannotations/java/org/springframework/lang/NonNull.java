// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/master/spring-core/src/main/java/org/springframework/lang/NonNull.java

package org.springframework.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NonNull {}
