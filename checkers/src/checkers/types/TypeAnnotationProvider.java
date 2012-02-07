package checkers.types;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.Tree;

/**
 * A {@link TypeAnnotationProvider} maps AST Trees to annotated types, represented
 * as set of {@link AnnotationMirror}s.
 */
public interface TypeAnnotationProvider {

    /**
     * Returns the annotated type of the argument tree, or
     * throws an {@link IllegalArgumentException} if the tree cannot
     * be typed.
     *
     * @param tree   an AST tree to be typed
     *
     * @return  the type of tree, represented as a set of AnnotationMirrors.
     * @throws  {@link IllegalArgumentException} if the argument cannot be typed.
     */
    public /*@NonNull*/ Set<AnnotationMirror> getAnnotations(Tree tree)
        throws IllegalArgumentException;

}
