package tests.nontopdefault.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultInUncheckedCodeFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

@SubtypeOf({NTDMiddle.class, NTDSide.class})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND})
@DefaultInUncheckedCodeFor({TypeUseLocation.LOWER_BOUND})
@DefaultFor({TypeUseLocation.LOWER_BOUND})
@ImplicitFor(
    typeNames = Void.class,
    literals = {LiteralKind.NULL}
)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface NTDBottom {}
