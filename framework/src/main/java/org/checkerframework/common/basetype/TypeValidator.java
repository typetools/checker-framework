package org.checkerframework.common.basetype;

import com.sun.source.tree.Tree;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * TypeValidator ensures that a type for a given tree is valid both for the tree and the type system
 * that is being used to check the tree.
 */
public interface TypeValidator {

    /**
     * The entry point to the type validator. Validate the type against the given tree.
     *
     * @param type the type to validate
     * @param tree the tree from which the type originated. If the tree is a method tree, then
     *     validate its return type. If the tree is a variable tree, then validate its field type.
     * @return true, iff the type is valid
     */
    public boolean isValid(AnnotatedTypeMirror type, Tree tree);
}
