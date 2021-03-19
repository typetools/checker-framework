package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;

public class TypeAnnotationMover extends VoidVisitorAdapter<Void> {
    private Map<String, TypeElement> allAnnotations;
    private Elements elements;

    public TypeAnnotationMover(Map<String, TypeElement> allAnnotations, Elements elements) {
        this.allAnnotations = new HashMap<>(allAnnotations);
        this.elements = elements;
    }

    @Override
    public void visit(FieldDeclaration node, Void p) {
        // When adding annotations, it should be sufficient to add the annotations to the type of
        // the first declared variable in the field declaration.
        Type type = node.getVariable(0).getType();
        if (isMultiPartName(type)) {
            return;
        }

        List<AnnotationExpr> annosToMove = getAnnotationsToMove(node, ElementType.FIELD);
        if (annosToMove.isEmpty()) {
            return;
        }

        if (!type.isClassOrInterfaceType()) {
            return;
        }

        removeAnnotations(node, annosToMove);
        annosToMove.forEach(anno -> type.asClassOrInterfaceType().addAnnotation(anno));
    }

    @Override
    public void visit(MethodDeclaration node, Void p) {
        Type type = node.getType();
        if (isMultiPartName(type)) {
            return;
        }

        List<AnnotationExpr> annosToMove = getAnnotationsToMove(node, ElementType.METHOD);
        if (annosToMove.isEmpty()) {
            return;
        }

        if (!type.isClassOrInterfaceType()) {
            return;
        }

        removeAnnotations(node, annosToMove);
        annosToMove.forEach(anno -> type.asClassOrInterfaceType().addAnnotation(anno));
    }

    private List<AnnotationExpr> getAnnotationsToMove(
            NodeWithAnnotations<?> node, ElementType declarationType) {
        List<AnnotationExpr> annosToMove = new ArrayList<>();
        for (AnnotationExpr annotation : node.getAnnotations()) {
            if (!isPossiblyDeclarationAnnotation(annotation, declarationType)) {
                annosToMove.add(annotation);
            }
        }

        return annosToMove;
    }

    private void removeAnnotations(
            NodeWithAnnotations<?> node, List<AnnotationExpr> annosToRemove) {
        NodeList<AnnotationExpr> newAnnos = new NodeList<>(node.getAnnotations());
        newAnnos.removeIf(anno -> annosToRemove.contains(anno));
        node.setAnnotations(newAnnos);
    }

    private @Nullable TypeElement getAnnotationDeclaration(AnnotationExpr annotation) {
        // TODO: Rewrite comments so not specefic to AnnotationFileParser.
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @FullyQualifiedName String annoNameFq = annotation.getNameAsString();
        TypeElement annoTypeElt = allAnnotations.get(annoNameFq);
        if (annoTypeElt == null) {
            // If the annotation was not imported, then #getAllAnnotations did not add it to the
            // allAnnotations field. This code adds the annotation when it is encountered
            // (i.e. here).
            // Note that this does not call AnnotationFileParser#getTypeElement to avoid a spurious
            // diagnostic if the annotation is actually unknown.
            annoTypeElt = elements.getTypeElement(annoNameFq);
            if (annoTypeElt == null) {
                // Not a supported annotation -> ignore
                return null;
            }
            InsertAjavaAnnotations.putAllNew(
                    allAnnotations,
                    InsertAjavaAnnotations.createNameToAnnotationMap(
                            Collections.singletonList(annoTypeElt)));
        }

        return annoTypeElt;
    }

    private boolean isPossiblyDeclarationAnnotation(
            AnnotationExpr annotation, ElementType declarationType) {
        TypeElement annotationType = getAnnotationDeclaration(annotation);
        if (annotationType == null) {
            return true;
        }

        return isPossiblyDeclarationAnnotation(annotationType, declarationType);
    }

    private boolean isPossiblyDeclarationAnnotation(
            TypeElement annotationDeclaration, ElementType declarationType) {
        Target target = annotationDeclaration.getAnnotation(Target.class);
        if (target == null) {
            return true;
        }

        boolean hasTypeUse = false;
        for (ElementType elementType : target.value()) {
            if (elementType == ElementType.TYPE_USE) {
                hasTypeUse = true;
            }

            if (elementType == declarationType) {
                return true;
            }
        }

        return !hasTypeUse;
    }

    /**
     * Returns whether {@code type} has a name containing multiple parts separated by dots, e.g.
     * "java.lang.String".
     *
     * @param type a JavaParser type node
     * @return true if {@code type} has a multi-part name
     */
    private boolean isMultiPartName(Type type) {
        return type.isClassOrInterfaceType()
                && type.asClassOrInterfaceType().getScope().isPresent();
    }
}
