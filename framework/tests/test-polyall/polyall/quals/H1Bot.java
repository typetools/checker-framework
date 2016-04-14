package polyall.quals;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@SubtypeOf({H1S1.class, H1S2.class, H1Invalid.class})
@ImplicitFor(literals = LiteralKind.NULL)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor({ TypeUseLocation.LOWER_BOUND })
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface H1Bot {}
