package polyall.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

@TypeQualifier
@SubtypeOf({H2S1.class, H2S2.class})
@ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@DefaultFor(DefaultLocation.LOWER_BOUNDS)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface H2Bot {}
