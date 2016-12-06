package testlib.compound.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({CCTop.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface CCBottom {}
