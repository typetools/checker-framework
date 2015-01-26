package org.checkerframework.framework.qual;

/*>>>
import org.checkerframework.checker.nullness.qual.Nullable;
*/

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.lang.model.type.TypeKind;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

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
     * @return {@link Class}es (subtypes of {@link AnnotatedTypeMirror}) of types
     *         for which an annotation should be implicitly added
     */
    Class<? extends AnnotatedTypeMirror>[] typeClasses() default {};

    /**
     * @return {@link Class}es (in the actual program) for which an annotation
     *         should be implicitly added.
     *         For example, "java.lang.Void.class" should receive the same annotation
     *         as the null literal.
     */
    Class<?>[] typeNames() default {};

    /**
     * @return Regular expressions of string literals, the types of which
     *         an annotation should be implicitly added
     */
    String[] stringPatterns() default {};

    // TODO: do we need an option to provide implicits for locations
    // specified by a DefaultLocation (which should then be renamed)?
}
