package checkers.oigj.quals;

import java.lang.annotation.*;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
//@PolymorphicQualifier // TODO: uncomment later
@SubtypeOf(World.class)
public @interface O {}
