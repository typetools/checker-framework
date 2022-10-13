package qual;

import org.checkerframework.checker.fenum.qual.FenumTop;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FenumTop.class)
// This weird default is used in this example to test whether defaults are
// correctly applied to both annotated and unannotated instances
// (in this example, "short" and "@NonNegative short", see issue #333).
@DefaultFor(types = {short.class})
public @interface MyFenum {}
