package checkers.regex.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.PolymorphicQualifier;
import checkers.quals.TypeQualifier;

/**
 * A polymorphic qualifier for the Regex type system.
 *
 * <p>
 * Any method written using {@code @PolyRegex} conceptually has two versions:
 * one in which every instance of {@code @PolyRegex String} has been replaced
 * by {@code @Regex String}, and one in which every instance of
 * {@code @PolyRegex String} has been replaced by {@code String}.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyRegex {}
