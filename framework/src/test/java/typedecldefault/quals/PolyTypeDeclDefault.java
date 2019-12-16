package typedecldefault.quals;

import java.lang.annotation.*;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/** A polymorphic qualifier for the TyepDeclDefault type system. */
@Documented
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyTypeDeclDefault {}
