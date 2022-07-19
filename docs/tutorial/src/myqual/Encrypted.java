package myqual;

import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Denotes that the representation of an object is encrypted. */
@Documented
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@QualifierForLiterals(stringPatterns = "^$")
@SubtypeOf(PossiblyUnencrypted.class)
public @interface Encrypted {}
