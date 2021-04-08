// Upstream version (this is a clean-room reimplementation of its interface):
// https://android.googlesource.com/platform/frameworks/support/+/master/annotations/src/main/java/android/support/annotation/Nullable.java

package android.support.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
  ElementType.METHOD,
  ElementType.PARAMETER,
  ElementType.FIELD,
  ElementType.LOCAL_VARIABLE,
  ElementType.ANNOTATION_TYPE,
  ElementType.PACKAGE
})
public @interface Nullable {}
