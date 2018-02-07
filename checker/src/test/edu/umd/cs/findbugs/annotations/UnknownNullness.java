// Upstream version:
// http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/UnknownNullness.html

package edu.umd.cs.findbugs.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface UnknownNullness {}
