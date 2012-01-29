package utilMDE;

import java.lang.annotation.*;

/**
 * Indicates that the annotated field is set via command line option.
 * Takes a single string argument that describes the option.  The string
 * is in the format '[-c] [type] description' where '-c' is a single
 * character short name for the option, 'type' is a description of the
 * option more specific than its java type (eg, filename) and 'description'
 * is a description of the option suitable for a usage printout.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
  String value();
}
