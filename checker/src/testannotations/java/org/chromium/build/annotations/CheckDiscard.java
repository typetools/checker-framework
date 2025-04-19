// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/CheckDiscard.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Causes build to assert that annotated classes / methods / fields are optimized away in release
 * builds (without dcheck_always_on).
 */
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface CheckDiscard {
  /**
   * Describes why the element should be discarded.
   *
   * @return reason for discarding (crbug links are preferred unless reason is trivial).
   */
  String value();
}
