package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

/**
 * A visitor that adds all annotations from a {@code AnnotatedTypeMirror} to the corresponding
 * JavaParser type, including nested types like array components.
 *
 * <p>The {@code AnnotatedTypeMirror} is passed as the secondary parameter to the visit methods.
 */
public class AnnotationTransferVisitor extends VoidVisitorAdapter<AnnotatedTypeMirror> {
    @Override
    public void visit(ArrayType target, AnnotatedTypeMirror type) {
        target.getComponentType().accept(this, ((AnnotatedArrayType) type).getComponentType());
        transferAnnotations(type, target);
    }

    @Override
    public void visit(ClassOrInterfaceType target, AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
            if (target.getTypeArguments().isPresent()) {
                NodeList<Type> types = target.getTypeArguments().get();
                for (int i = 0; i < types.size(); i++) {
                    types.get(i).accept(this, declaredType.getTypeArguments().get(i));
                }
            }
        }

        transferAnnotations(type, target);
    }

    @Override
    public void visit(PrimitiveType target, AnnotatedTypeMirror type) {
        transferAnnotations(type, target);
    }

    @Override
    public void visit(TypeParameter target, AnnotatedTypeMirror type) {
        AnnotatedTypeVariable annotatedTypeVar = (AnnotatedTypeVariable) type;
        NodeList<ClassOrInterfaceType> bounds = target.getTypeBound();
        if (bounds.size() == 1) {
            bounds.get(0).accept(this, annotatedTypeVar.getUpperBound());
        }
    }

    /**
     * Transfers annotations from {@code annotatedType} to {@code target}. Does nothing if {@code
     * annotatedType} is null.
     *
     * @param annotatedType type with annotations to transfer
     * @param target JavaParser node representing the type to transfer annotations to
     */
    private void transferAnnotations(
            @Nullable AnnotatedTypeMirror annotatedType, NodeWithAnnotations<?> target) {
        if (annotatedType == null) {
            return;
        }

        for (AnnotationMirror annotation : annotatedType.getAnnotations()) {
            AnnotationExpr convertedAnnotation =
                    AnnotationMirrorToAnnotationExprConversion.annotationMirrorToAnnotationExpr(
                            annotation);
            target.addAnnotation(convertedAnnotation);
        }
    }
}
