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
import org.checkerframework.framework.stub.AnnotationFileParser;

/**
 * Moves annotations in a JavaParser AST from declaration position onto the types they correspond
 * to.
 *
 * <p>When parsing a method or file in Java such as {@code @Tainted String myField}, JavaParser
 * doesn't know if {@code @Tainted} belongs to the field declaration itself or to the type {@code
 * String}, so it makes it a declaration annotation. Where the annotation actually belongs depends
 * on the type of the annotation, which JavaParser doesn't have access to.
 *
 * <p>For each such annotation, this class checks if the current instance of Java recognizes it.
 * Since this file should be run as part of the Checker Framework, in particular this will include
 * all Checker Framework annotations. If it recognizes the annotation, and it can only appear on the
 * field or method type and not the declaration, then it moves the annotation to the type position.
 */
public class TypeAnnotationMover extends VoidVisitorAdapter<Void> {
    /**
     * Annotations imported by the file, stored as a mapping from names to the TypeElements for the
     * annotations. When checking an annotation in the file, the annotations in this field determine
     * if the annotation was imported or not.
     */
    private Map<String, TypeElement> allAnnotations;
    /** Utility class for working with Elements. */
    private Elements elements;

    /**
     * Constructs a {@code TypeAnnotationMover}. The {@code allAnnotations} parameter should contain
     * all the annotations imported by the file to be visited. When examining an annotation in the
     * file, looks up the name in {@code allAnnotations} to find the TypeElement for the annotation.
     *
     * @param allAnnotations mapping from annotation names to TypeElements for the annotations for
     *     each annotation imported by the file
     * @param elements instance of {@code Element}s
     */
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

    /**
     * Given a JavaParser node for a declaration and the type of Element that declaration
     * represents, returns a List of annotations currently in declaration position that can't
     * possibly be declaration annotations for that type of declaration.
     *
     * @param node JavaParser node for declaration
     * @param declarationType the type of declaration {@code node} represents
     * @return a list of annotations in declaration position that should be on the declaration's
     *     type
     */
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

    /**
     * Removes all annotations from {@code node} that appear in {@code annosToRemove}
     *
     * @param node a node with annotations
     * @param annosToRemove list of annotations to remove
     */
    private void removeAnnotations(
            NodeWithAnnotations<?> node, List<AnnotationExpr> annosToRemove) {
        NodeList<AnnotationExpr> newAnnos = new NodeList<>(node.getAnnotations());
        newAnnos.removeIf(anno -> annosToRemove.contains(anno));
        node.setAnnotations(newAnnos);
    }

    /**
     * Returns the TypeElement for an annotation if it could be found and null otherwise.
     *
     * <p>If {@code annotation} was listed in the file's imports, returns its value in {@link
     * #allAnnotations}. If it wasn't imported but its element could still be found, adds the new
     * TypeElement to {@link #allAnnotations} and returns it.
     *
     * @param annotation a JavaParser annotation
     * @return the TypeElement for {@code annotation} if it could be found, null otherwise
     */
    private @Nullable TypeElement getAnnotationDeclaration(AnnotationExpr annotation) {
        @SuppressWarnings("signature") // https://tinyurl.com/cfissue/3094
        @FullyQualifiedName String annoNameFq = annotation.getNameAsString();
        TypeElement annoTypeElt = allAnnotations.get(annoNameFq);
        if (annoTypeElt == null) {
            annoTypeElt = elements.getTypeElement(annoNameFq);
            if (annoTypeElt == null) {
                // Not a supported annotation.
                return null;
            }
            AnnotationFileParser.putAllNew(
                    allAnnotations,
                    AnnotationFileParser.createNameToAnnotationMap(
                            Collections.singletonList(annoTypeElt)));
        }

        return annoTypeElt;
    }

    /**
     * Returns if {@code annotation} could be declaration annotation for {@code declarationType}.
     * This would be the case if the annotation isn't recognized at all, or if it was recognized and
     * has {@code declarationType} as one of its targets.
     *
     * @param annotation a JavaParser annotation expression
     * @param declarationType the declaration type to check if {@code annotation} might be a
     *     declaration annotation for
     * @return false unless {@code annotation} definitely cannot be a declaration annotation for
     *     {@code declarationType}
     */
    private boolean isPossiblyDeclarationAnnotation(
            AnnotationExpr annotation, ElementType declarationType) {
        TypeElement annotationType = getAnnotationDeclaration(annotation);
        if (annotationType == null) {
            return true;
        }

        return isPossiblyDeclarationAnnotation(annotationType, declarationType);
    }

    /**
     * Returns whether the annotation represented by {@code annotationDeclaration} might be a
     * declaration for {@code declarationType}. This holds if {@code declarationType} is a target of
     * the annotation, or if {@code ElementType.TYPE_USE} is not a target of the annotation.
     *
     * @param annotationDeclaration declaration for an annotation
     * @param declarationType the declaration type to check if the annotation might be a declaration
     *     annotation for
     * @return true if {@code annotationDeclaration} contains {@code declarationType} as a target or
     *     doesn't contain {@code ElemenType.TYPE_USE} as a target.
     */
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
