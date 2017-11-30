package org.checkerframework.framework.flow;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.trees.TreeBuilder;

/**
 * The TreeBuilder permits the creation of new AST Trees using the non-public Java compiler API
 * TreeMaker. Initially, it will support construction of desugared Trees required by the CFGBuilder,
 * e.g. the pieces of a desugared enhanced for loop.
 */
public class CFTreeBuilder extends TreeBuilder {

    public CFTreeBuilder(ProcessingEnvironment env) {
        super(env);
    }

    /**
     * Builds an AST Tree representing an AnnotatedTypeMirror.
     *
     * @param annotatedType the annotated type
     * @return a Tree representing the annotated type
     */
    public Tree buildAnnotatedType(AnnotatedTypeMirror annotatedType) {
        return createAnnotatedType(annotatedType);
    }

    private Tree createAnnotatedType(AnnotatedTypeMirror annotatedType) {
        // Implementation based on com.sun.tools.javac.tree.TreeMaker.Type

        // Convert the annotations from a set of AnnotationMirrors
        // to a list of AnnotationTrees.
        Set<AnnotationMirror> annotations = annotatedType.getAnnotations();
        List<JCTree.JCAnnotation> annotationTrees = List.nil();

        for (AnnotationMirror am : annotations) {
            // TODO: what TypeAnnotationPosition should be used?
            Attribute.TypeCompound typeCompound =
                    TypeAnnotationUtils.createTypeCompoundFromAnnotationMirror(
                            am, TypeAnnotationUtils.unknownTAPosition(), env);
            JCTree.JCAnnotation annotationTree = maker.Annotation(typeCompound);
            JCTree.JCAnnotation typeAnnotationTree =
                    maker.TypeAnnotation(
                            annotationTree.getAnnotationType(), annotationTree.getArguments());

            typeAnnotationTree.attribute = typeCompound;

            annotationTrees = annotationTrees.append(typeAnnotationTree);
        }

        // Convert the underlying type from a TypeMirror to an
        // ExpressionTree and combine with the AnnotationTrees
        // to form a ClassTree of kind ANNOTATION_TYPE.
        Tree underlyingTypeTree;
        switch (annotatedType.getKind()) {
            case BYTE:
                underlyingTypeTree = maker.TypeIdent(TypeTag.BYTE);
                break;
            case CHAR:
                underlyingTypeTree = maker.TypeIdent(TypeTag.BYTE);
                break;
            case SHORT:
                underlyingTypeTree = maker.TypeIdent(TypeTag.SHORT);
                break;
            case INT:
                underlyingTypeTree = maker.TypeIdent(TypeTag.INT);
                break;
            case LONG:
                underlyingTypeTree = maker.TypeIdent(TypeTag.LONG);
                break;
            case FLOAT:
                underlyingTypeTree = maker.TypeIdent(TypeTag.FLOAT);
                break;
            case DOUBLE:
                underlyingTypeTree = maker.TypeIdent(TypeTag.DOUBLE);
                break;
            case BOOLEAN:
                underlyingTypeTree = maker.TypeIdent(TypeTag.BOOLEAN);
                break;
            case VOID:
                underlyingTypeTree = maker.TypeIdent(TypeTag.VOID);
                break;
            case TYPEVAR:
                {
                    // No recursive annotations.
                    AnnotatedTypeMirror.AnnotatedTypeVariable variable =
                            (AnnotatedTypeMirror.AnnotatedTypeVariable) annotatedType;
                    TypeVariable underlyingTypeVar = variable.getUnderlyingType();
                    underlyingTypeTree =
                            maker.Ident((Symbol.TypeSymbol) (underlyingTypeVar).asElement());
                    break;
                }
            case WILDCARD:
                {
                    AnnotatedTypeMirror.AnnotatedWildcardType wildcard =
                            (AnnotatedTypeMirror.AnnotatedWildcardType) annotatedType;
                    WildcardType wildcardType = wildcard.getUnderlyingType();
                    if (wildcardType.getExtendsBound() != null) {
                        Tree annotatedExtendsBound =
                                createAnnotatedType(wildcard.getExtendsBound());
                        underlyingTypeTree =
                                maker.Wildcard(
                                        maker.TypeBoundKind(BoundKind.EXTENDS),
                                        (JCTree) annotatedExtendsBound);
                    } else if (wildcardType.getSuperBound() != null) {
                        Tree annotatedSuperBound = createAnnotatedType(wildcard.getSuperBound());
                        underlyingTypeTree =
                                maker.Wildcard(
                                        maker.TypeBoundKind(BoundKind.SUPER),
                                        (JCTree) annotatedSuperBound);
                    } else {
                        underlyingTypeTree =
                                maker.Wildcard(maker.TypeBoundKind(BoundKind.UNBOUND), null);
                    }
                    break;
                }
            case DECLARED:
                {
                    underlyingTypeTree = maker.Type((Type) annotatedType.getUnderlyingType());

                    if (underlyingTypeTree instanceof JCTree.JCTypeApply) {
                        // Replace the type parameters with annotated versions.
                        AnnotatedTypeMirror.AnnotatedDeclaredType annotatedDeclaredType =
                                (AnnotatedTypeMirror.AnnotatedDeclaredType) annotatedType;
                        List<JCTree.JCExpression> typeArgTrees = List.nil();
                        for (AnnotatedTypeMirror arg : annotatedDeclaredType.getTypeArguments()) {
                            typeArgTrees =
                                    typeArgTrees.append(
                                            (JCTree.JCExpression) createAnnotatedType(arg));
                        }
                        JCTree.JCExpression clazz =
                                (JCTree.JCExpression)
                                        ((JCTree.JCTypeApply) underlyingTypeTree).getType();
                        underlyingTypeTree = maker.TypeApply(clazz, typeArgTrees);
                    }
                    break;
                }
            case ARRAY:
                {
                    AnnotatedTypeMirror.AnnotatedArrayType annotatedArrayType =
                            (AnnotatedTypeMirror.AnnotatedArrayType) annotatedType;
                    Tree annotatedComponentTree =
                            createAnnotatedType(annotatedArrayType.getComponentType());
                    underlyingTypeTree =
                            maker.TypeArray((JCTree.JCExpression) annotatedComponentTree);
                    break;
                }
            case ERROR:
                underlyingTypeTree = maker.TypeIdent(TypeTag.ERROR);
                break;
            default:
                assert false : "unexpected type: " + annotatedType;
                underlyingTypeTree = null;
                break;
        }

        ((JCTree) underlyingTypeTree).setType((Type) annotatedType.getUnderlyingType());

        if (annotationTrees.isEmpty()) {
            return underlyingTypeTree;
        }

        JCTree.JCAnnotatedType annotatedTypeTree =
                maker.AnnotatedType(annotationTrees, (JCTree.JCExpression) underlyingTypeTree);
        annotatedTypeTree.setType((Type) annotatedType.getUnderlyingType());

        return annotatedTypeTree;
    }
}
