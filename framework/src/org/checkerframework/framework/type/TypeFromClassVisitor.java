package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;
import javax.lang.model.element.TypeElement;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Converts ClassTrees into AnnotatedDeclaredType.
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromClassVisitor extends TypeFromTreeVisitor {

    @Override
    public AnnotatedTypeMirror visitClass(ClassTree node, AnnotatedTypeFactory f) {
        TypeElement elt = TreeUtils.elementFromDeclaration(node);
        AnnotatedTypeMirror result = f.toAnnotatedType(elt.asType(), true);

        ElementAnnotationApplier.apply(result, elt, f);

        return result;
    }
}
