package checkers.nullness.quals;

import java.lang.annotation.*;

import checkers.quals.*;

/**
 * Indicates that the annotated reference of an Object that is a key in a map.
 *
 * <p>
 * The value of the annotation should be the reference name of the map.  The
 * following declaration for example:
 *
 * <pre><code>
 *   Map<String, String> config = ...;
 *   @KeyFor("config") String key = "HOSTNAME";
 *
 *   String hostname = config.get(key);
 * </code></pre>
 *
 * indicates that "HOSTNAME" should be a key in the config.  The Nullness
 * checker deduce this information to deduce that {@code hostname} reference
 * is a nonnull reference.
 *
 * <p>
 * <b>Limitation</b>: The Nullness checker trusts the user and doesn't
 * validate the annotations.  Future releases will check for the presence of
 * the key in the map (when possible).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface KeyFor {
    public String[] value();
}
