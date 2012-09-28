package checkers.quals;

import java.lang.annotation.*;

import javax.lang.model.type.TypeKind;

import checkers.nullness.quals.Nullable;
import checkers.types.*;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * A meta-annotation that specifies the trees and types for which the framework
 * should automatically add that qualifier. These types and trees can be
 * specified via any combination of four fields.
 *
 * <p>
 * For example, the {@link Nullable} annotation is annotated
 * with
 * <pre>
 *   &#064;ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
 * </pre>
 * to denote that
 * the framework should automatically apply {@link Nullable} to all instances
 * of "null."
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ImplicitFor {

    /**
     * @return {@link Kind}s of trees for which an annotation should be
     *         implicitly added
     */
    Tree.Kind[] trees() default {};

    /**
     * @return {@link Class}es of trees for which an annotation should be
     *         implicitly added
     */
    Class<? extends Tree>[] treeClasses() default {};

    /**
     * @return {@link TypeKind}s of types for which an annotation should be
     *         implicitly added
     */
    TypeKind[] types() default {};

    /**
     * @return {@link Class}es of types for which an annotation should be
     *         implicitly added
     */
    Class<? extends AnnotatedTypeMirror>[] typeClasses() default {};

    /**
     * @return Regular expressions of string literals, the types of which
     *         an annotation should be implicitly added
     */
    String[] stringPatterns() default {};
}
