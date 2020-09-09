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
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;

public class AnnotationTransferVisitor extends VoidVisitorAdapter<AnnotatedTypeMirror> {
    @Override
    public void visit(ArrayType target, AnnotatedTypeMirror type) {
        target.getComponentType().accept(this, ((AnnotatedArrayType) type).getComponentType());
        transferAnnotations(type, target);
    }

    @Override
    public void visit(ClassOrInterfaceType target, AnnotatedTypeMirror type) {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) type;
        if (target.getTypeArguments().isPresent()) {
            NodeList<Type> types = target.getTypeArguments().get();
            for (int i = 0; i < types.size(); i++) {
                types.get(i).accept(this, declaredType.getTypeArguments().get(i));
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
        // TODO: If there's not explicit bound in JavaParser, insert them rather than skipping them.
        if (bounds.size() == 1) {
            bounds.get(0).accept(this, annotatedTypeVar.getUpperBound());
        }
    }

    // TODO: Handle wildcard type?

    private void transferAnnotations(
            @Nullable AnnotatedTypeMirror annotatedType, NodeWithAnnotations<?> target) {
        if (annotatedType == null) {
            return;
        }

        for (AnnotationMirror annotation : annotatedType.getAnnotations()) {
            if (annotation == null) {
                continue;
            }

            AnnotationExpr convertedAnnotation =
                    AnnotationConversion.annotationMirrorToAnnotationExpr(annotation);
            target.addAnnotation(convertedAnnotation);
        }
    }
}
