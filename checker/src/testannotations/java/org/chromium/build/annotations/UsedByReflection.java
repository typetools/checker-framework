// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/UsedByReflection.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation used for marking methods and fields that are called by reflection. Useful for keeping
 * components that would otherwise be removed by Proguard. Use the value parameter to mention a file
 * that calls this method.
 *
 * <p>Note that adding this annotation to a method is not enough to guarantee that it is kept -
 * either its class must be referenced elsewhere in the program, or the class must be annotated with
 * this as well.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.CONSTRUCTOR})
@UsedReflectively
public @interface UsedByReflection {
  String value();
}
