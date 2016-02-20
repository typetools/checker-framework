package qual;

import org.checkerframework.framework.qual.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import com.sun.source.tree.Tree.Kind;

/**
 * Denotes that the representation of an object is encrypted.
 */
@SubtypeOf(PossiblyUnencrypted.class)
@ImplicitFor(trees = { Kind.NULL_LITERAL })
@DefaultFor({TypeUseLocation.LOWER_BOUND})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Encrypted {}
