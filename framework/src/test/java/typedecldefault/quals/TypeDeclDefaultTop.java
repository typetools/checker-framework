package typedecldefault.quals;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/** This is the top qualifier of the TypeDeclDefault type system. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultFor({TypeUseLocation.TYPE_DECLARATION, TypeUseLocation.CONSTRUCTOR_RESULT})
public @interface TypeDeclDefaultTop {}
