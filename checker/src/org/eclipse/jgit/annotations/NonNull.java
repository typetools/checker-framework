// Upstream version:
// https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit/src/org/eclipse/jgit/annotations/NonNull.java

package org.eclipse.jgit.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface NonNull {}
