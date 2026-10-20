// Upstream version (this is a clean-room reimplementation of its interface):
// https://source.chromium.org/chromium/chromium/src/+/main:build/android/java/src/org/chromium/build/annotations/Contract.java

package org.chromium.build.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * See: https://github.com/uber/NullAway/wiki/Supported-Annotations#contracts
 *
 * <p>Not directly using NullAway's annotations so that Cronet does not need the extra dep.
 *
 * <pre>
 * Examples:
 * // The contract is: "If the parameter is null, the method will return false".
 * // NullAway infers nullness from the inverse: Returning "true" means the parameter is non-null.
 * @Contract("null -> false")
 * // Returning false means the second parameter is non-null.
 * @Contract("_, null -> true")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Contract {
  String value();
}
