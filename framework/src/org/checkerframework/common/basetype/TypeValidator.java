package org.checkerframework.common.basetype;

import com.sun.source.tree.Tree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * TypeValidator ensures that a type for a given tree is valid both for the tree
 * and the type system that is being used to check the tree.
 */
public interface TypeValidator {

    /** The entry point to the type validator.
     * Validate the type against the given tree.
     *
     * @param type The type to validate.
     * @param tree The tree from which the type originated.
     *   Note that the tree might be a method tree - the
     *     return type should then be validated.
     *   Note that the tree might be a variable tree - the
     *     field type should then be validated.
     * @return True, iff the type is valid.
     */
    public boolean isValid(AnnotatedTypeMirror type, Tree tree);
}
