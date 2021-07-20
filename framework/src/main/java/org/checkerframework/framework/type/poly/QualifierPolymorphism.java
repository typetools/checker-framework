package org.checkerframework.framework.type.poly;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

import javax.lang.model.element.VariableElement;

/**
 * Interface to implement qualifier polymorphism.
 *
 * @see PolymorphicQualifier
 * @see AbstractQualifierPolymorphism
 * @see DefaultQualifierPolymorphism
 */
public interface QualifierPolymorphism {

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate; is side-effected by this method
     */
    void resolve(MethodInvocationTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param tree the tree associated with the type
     * @param type the type to annotate; is side-effected by this method
     */
    void resolve(NewClassTree tree, AnnotatedExecutableType type);

    /**
     * Resolves polymorphism annotations for the given type.
     *
     * @param functionalInterface the function type of {@code memberReference}
     * @param memberReference the type of a member reference; is side-effected by this method
     */
    void resolve(
            AnnotatedExecutableType functionalInterface, AnnotatedExecutableType memberReference);

    /**
     * Resolves polymorphism annotations for the given field type.
     *
     * @param field field element to whose poly annotation must be resolved
     * @param owner the type of the object whose field is being typed
     * @param type type of the field which still has poly annotations
     */
    void resolve(VariableElement field, AnnotatedTypeMirror owner, AnnotatedTypeMirror type);
}
