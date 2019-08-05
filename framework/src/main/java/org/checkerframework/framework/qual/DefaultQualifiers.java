package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the annotations to be included in a type without having to provide them explicitly.
 *
 * <p>This annotation permits specifying multiple default qualifiers for more than one type system.
 * It is generally not used by programmers, because Java permits writing multiple annotations of the
 * same name at a single location and internally creates this wrapper annotation.
 *
 * <p>Example: The following two code snippets have identical effect.
 * <!-- &nbsp; is a hack that prevents @ from being the first character on the line, which confuses Javadoc -->
 *
 * <pre>
 * &nbsp; @DefaultQualifier(NonNull.class)
 * &nbsp; @DefaultQualifier(value = Interned.class, locations = ALL_EXCEPT_LOCALS)
 * &nbsp; @DefaultQualifier(Tainted.class)
 * &nbsp; class MyClass { ... }
 * </pre>
 *
 * <pre>
 * &nbsp; @DefaultQualifiers({
 * &nbsp;     @DefaultQualifier(NonNull.class),
 * &nbsp;     @DefaultQualifier(value = Interned.class, locations = ALL_EXCEPT_LOCALS),
 * &nbsp;     @DefaultQualifier(Tainted.class)
 * &nbsp; })
 * &nbsp; class MyClass { ... }
 * </pre>
 *
 * This annotation currently has no effect in stub files.
 *
 * @see DefaultQualifier
 * @checker_framework.manual #defaults Default qualifier for unannotated types
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
    ElementType.PACKAGE,
    ElementType.TYPE,
    ElementType.CONSTRUCTOR,
    ElementType.METHOD,
    ElementType.FIELD,
    ElementType.LOCAL_VARIABLE,
    ElementType.PARAMETER
})
public @interface DefaultQualifiers {
    /** The default qualifier settings. */
    DefaultQualifier[] value() default {};
}
