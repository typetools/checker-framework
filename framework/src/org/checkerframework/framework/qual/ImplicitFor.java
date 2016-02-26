package org.checkerframework.framework.qual;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

/**
 * A meta-annotation that specifies the trees and types for which the framework
 * should automatically add that qualifier. These types and trees can be
 * specified via any combination of six attributes.
 *
 * <p>
 * For example, the {@code Nullable} annotation is annotated
 * with
 * <pre>
 *   &#064;ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
 * </pre>
 * to denote that
 * the framework should automatically apply {@code Nullable} to all instances
 * of "null."
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ImplicitFor {

    /**
     * @return {@link LiteralKind}s for which an annotation should be
     *         implicitly added
     */
    LiteralKind[] literals() default {};

    /**
     * @return {@link TypeKind}s of types for which an annotation should be
     *         implicitly added
     */
    TypeKind[] types() default {};

    /**
     * @return {@link Class}es (in the actual program) for which an annotation
     *         should be implicitly added.
     *         For example, "java.lang.Void.class" should receive the same annotation
     *         as the null literal.
     */
    Class<?>[] typeNames() default {};

    /**
     * @return Regular expressions of string literals, the types of which
     *         an annotation should be implicitly added.
     *         If multiple patterns match, then the string literal is given the
     *         greatest lower bound of all the matches.
     */
    String[] stringPatterns() default {};

    // TODO: do we need an option to provide implicits for locations
    // specified by a TypeUseLocation (which should then be renamed)?
}
