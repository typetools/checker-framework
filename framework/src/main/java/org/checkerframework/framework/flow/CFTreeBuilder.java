package org.checkerframework.framework.flow;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.util.List;

import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * The TreeBuilder permits the creation of new AST Trees using the non-public Java compiler API
 * TreeMaker. Initially, it will support construction of desugared Trees required by the CFGBuilder,
 * e.g. the pieces of a desugared enhanced for loop.
 */
public class CFTreeBuilder extends TreeBuilder {

    /**
     * To avoid infinite recursions, record each wildcard that has been converted to a tree. This
     * set is cleared each time {@link #buildAnnotatedType(TypeMirror)} is called.
     */
    private final Set<WildcardType> visitedWildcards = new HashSet<>();

    /**
     * Creates a {@code CFTreeBuilder}.
     *
     * @param env environment
     */
    public CFTreeBuilder(ProcessingEnvironment env) {
        super(env);
    }

    /**
     * Builds an AST Tree representing a type, including AnnotationTrees for its annotations.
     *
     * @param type the type
     * @return a Tree representing the type
     */
    public Tree buildAnnotatedType(TypeMirror type) {
        visitedWildcards.clear();
        return createAnnotatedType(type);
    }

    /**
     * Converts a list of AnnotationMirrors to the a corresponding list of new AnnotationTrees.
     *
     * @param annotations the annotations
     * @return new annotation trees representing the annotations
     */
    private List<JCAnnotation> convertAnnotationMirrorsToAnnotationTrees(
            Collection<? extends AnnotationMirror> annotations) {
        List<JCAnnotation> annotationTrees = List.nil();

        for (AnnotationMirror am : annotations) {
            // TODO: what TypeAnnotationPosition should be used?
            Attribute.TypeCompound typeCompound =
                    TypeAnnotationUtils.createTypeCompoundFromAnnotationMirror(
                            am, TypeAnnotationUtils.unknownTAPosition(), env);
            JCAnnotation annotationTree = maker.Annotation(typeCompound);
            JCAnnotation typeAnnotationTree =
                    maker.TypeAnnotation(
                            annotationTree.getAnnotationType(), annotationTree.getArguments());

            typeAnnotationTree.attribute = typeCompound;

            annotationTrees = annotationTrees.append(typeAnnotationTree);
        }
        return annotationTrees;
    }

    /**
     * Builds an AST Tree representing a type, including AnnotationTrees for its annotations. This
     * internal method differs from the public {@link #buildAnnotatedType(TypeMirror)} only in that
     * it does not reset the list of visited wildcards.
     *
     * @param type the type for which to create a tree
     * @return a Tree representing the type
     */
    private Tree createAnnotatedType(TypeMirror type) {
        // Implementation based on com.sun.tools.javac.tree.TreeMaker.Type

        // Convert the annotations from a set of AnnotationMirrors
        // to a list of AnnotationTrees.
        java.util.List<? extends AnnotationMirror> annotations = type.getAnnotationMirrors();
        List<JCAnnotation> annotationTrees = convertAnnotationMirrorsToAnnotationTrees(annotations);

        // Convert the underlying type from a TypeMirror to an ExpressionTree and combine with the
        // AnnotationTrees to form a ClassTree of kind ANNOTATION_TYPE.
        JCExpression typeTree;
        switch (type.getKind()) {
            case BYTE:
                typeTree = maker.TypeIdent(TypeTag.BYTE);
                break;
            case CHAR:
                typeTree = maker.TypeIdent(TypeTag.CHAR);
                break;
            case SHORT:
                typeTree = maker.TypeIdent(TypeTag.SHORT);
                break;
            case INT:
                typeTree = maker.TypeIdent(TypeTag.INT);
                break;
            case LONG:
                typeTree = maker.TypeIdent(TypeTag.LONG);
                break;
            case FLOAT:
                typeTree = maker.TypeIdent(TypeTag.FLOAT);
                break;
            case DOUBLE:
                typeTree = maker.TypeIdent(TypeTag.DOUBLE);
                break;
            case BOOLEAN:
                typeTree = maker.TypeIdent(TypeTag.BOOLEAN);
                break;
            case VOID:
                typeTree = maker.TypeIdent(TypeTag.VOID);
                break;
            case TYPEVAR:
                // No recursive annotations.
                TypeVariable underlyingTypeVar = (TypeVariable) type;
                typeTree = maker.Ident((TypeSymbol) underlyingTypeVar.asElement());
                break;
            case WILDCARD:
                WildcardType wildcardType = (WildcardType) type;
                boolean visitedBefore = !visitedWildcards.add(wildcardType);
                if (!visitedBefore && wildcardType.getExtendsBound() != null) {
                    Tree annotatedExtendsBound =
                            createAnnotatedType(wildcardType.getExtendsBound());
                    typeTree =
                            maker.Wildcard(
                                    maker.TypeBoundKind(BoundKind.EXTENDS),
                                    (JCTree) annotatedExtendsBound);
                } else if (!visitedBefore && wildcardType.getSuperBound() != null) {
                    Tree annotatedSuperBound = createAnnotatedType(wildcardType.getSuperBound());
                    typeTree =
                            maker.Wildcard(
                                    maker.TypeBoundKind(BoundKind.SUPER),
                                    (JCTree) annotatedSuperBound);
                } else {
                    typeTree = maker.Wildcard(maker.TypeBoundKind(BoundKind.UNBOUND), null);
                }
                break;
            case INTERSECTION:
                IntersectionType intersectionType = (IntersectionType) type;
                List<JCExpression> components = List.nil();
                for (TypeMirror bound : intersectionType.getBounds()) {
                    components = components.append((JCExpression) createAnnotatedType(bound));
                }
                typeTree = maker.TypeIntersection(components);
                break;
                // case UNION:
                // TODO: case UNION similar to INTERSECTION, but write test first.
            case DECLARED:
                typeTree = maker.Type((Type) type);

                if (typeTree instanceof JCTypeApply) {
                    // Replace the type parameters with annotated versions.
                    DeclaredType annotatedDeclaredType = (DeclaredType) type;
                    List<JCExpression> typeArgTrees = List.nil();
                    for (TypeMirror arg : annotatedDeclaredType.getTypeArguments()) {
                        typeArgTrees = typeArgTrees.append((JCExpression) createAnnotatedType(arg));
                    }
                    JCExpression clazz = (JCExpression) ((JCTypeApply) typeTree).getType();
                    typeTree = maker.TypeApply(clazz, typeArgTrees);
                }
                break;
            case ARRAY:
                ArrayType arrayType = (ArrayType) type;
                Tree componentTree = createAnnotatedType(arrayType.getComponentType());
                typeTree = maker.TypeArray((JCExpression) componentTree);
                break;
            case ERROR:
                typeTree = maker.TypeIdent(TypeTag.ERROR);
                break;
            default:
                assert false : "unexpected type: " + type;
                typeTree = null;
                break;
        }

        typeTree.setType((Type) type);

        if (annotationTrees.isEmpty()) {
            return typeTree;
        }

        JCAnnotatedType annotatedTypeTree = maker.AnnotatedType(annotationTrees, typeTree);
        annotatedTypeTree.setType((Type) type);

        return annotatedTypeTree;
    }
}
