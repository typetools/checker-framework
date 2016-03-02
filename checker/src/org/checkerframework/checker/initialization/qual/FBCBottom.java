package org.checkerframework.checker.initialization.qual;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link FBCBottom} marks the bottom of the Freedom Before Commitment type
 * hierarchy.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @author Stefan Heule
 */
@SubtypeOf({ UnderInitialization.class, Initialized.class })
@ImplicitFor(literals = { LiteralKind.NULL })
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND })
public @interface FBCBottom {
}
