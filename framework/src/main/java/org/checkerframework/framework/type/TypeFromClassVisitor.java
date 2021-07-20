package org.checkerframework.framework.type;

import com.sun.source.tree.ClassTree;

import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.TypeElement;

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
