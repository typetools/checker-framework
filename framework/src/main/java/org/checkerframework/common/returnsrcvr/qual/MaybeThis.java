package org.checkerframework.common.returnsrcvr.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.*;

/** The top type for the TemplateFora Checker's type system. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
@QualifierForLiterals(LiteralKind.NULL)
@DefaultFor(types = Void.class, value = TypeUseLocation.LOWER_BOUND)
public @interface MaybeThis {}
