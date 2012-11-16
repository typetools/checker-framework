package checkers.flow.analysis.checkers;

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeVariable;

import javacutils.trees.TreeBuilder;

import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.Tree;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

/**
 * The TreeBuilder permits the creation of new AST Trees using the
 * non-public Java compiler API TreeMaker.  Initially, it will support
 * construction of desugared Trees required by the CFGBuilder, e.g.
 * the pieces of a desugared enhanced for loop.
 */

public class CFTreeBuilder extends TreeBuilder {

    public CFTreeBuilder(ProcessingEnvironment env) {
        super(env);
    }

    /**
     * Builds an AST Tree representing an AnnotatedTypeMirror.
     *
     * @param annotatedType  the annotated type
     * @return  a Tree representing the annotated type
     */
    public Tree buildAnnotatedType(AnnotatedTypeMirror annotatedType) {
        return AnnotatedType(annotatedType);
    }

    private Tree AnnotatedType(AnnotatedTypeMirror annotatedType) {
        // Implementation based on com.sun.tools.javac.tree.TreeMaker.Type

        // Convert the annotations from a set of AnnotationMirrors
        // to a list of AnnotationTrees.
        Set<AnnotationMirror> annotations = annotatedType.getAnnotations();
        List<JCTree.JCTypeAnnotation> annotationTrees = List.nil();

        for (AnnotationMirror am : annotations) {
            // We can only make annotation trees out of annotations
            // that actually appear in an AST Tree.  Other annotations,
            // such as @Unqualified, are skipped.
            if (!AnnotatedTypeMirror.isUnqualified(am)) {
                Attribute.TypeCompound typeCompound =
                    attributeFromAnnotationMirror(am);

                JCTree.JCAnnotation annotationTree =
                    maker.Annotation(typeCompound);
                JCTree.JCTypeAnnotation typeAnnotationTree =
                    maker.TypeAnnotation(annotationTree.getAnnotationType(),
                                         annotationTree.getArguments());

                typeAnnotationTree.attribute_field = typeCompound;

                annotationTrees = annotationTrees.append(typeAnnotationTree);
            }
        }

        // Convert the underlying type from a TypeMirror to an
        // ExpressionTree and combine with the AnnotationTrees
        // to form a ClassTree of kind ANNOTATION_TYPE.
        Tree underlyingTypeTree;
        switch (annotatedType.getKind()) {
        case BYTE:
            underlyingTypeTree = maker.TypeIdent(TypeTags.BYTE);
            break;
        case CHAR:
            underlyingTypeTree = maker.TypeIdent(TypeTags.BYTE);
            break;
        case SHORT:
            underlyingTypeTree = maker.TypeIdent(TypeTags.SHORT);
            break;
        case INT:
            underlyingTypeTree = maker.TypeIdent(TypeTags.INT);
            break;
        case LONG:
            underlyingTypeTree = maker.TypeIdent(TypeTags.LONG);
            break;
        case FLOAT:
            underlyingTypeTree = maker.TypeIdent(TypeTags.FLOAT);
            break;
        case DOUBLE:
            underlyingTypeTree = maker.TypeIdent(TypeTags.DOUBLE);
            break;
        case BOOLEAN:
            underlyingTypeTree = maker.TypeIdent(TypeTags.BOOLEAN);
            break;
        case VOID:
            underlyingTypeTree = maker.TypeIdent(TypeTags.VOID);
            break;
        case TYPEVAR: {
            // No recursive annotations.
            AnnotatedTypeMirror.AnnotatedTypeVariable variable =
                (AnnotatedTypeMirror.AnnotatedTypeVariable) annotatedType;
            TypeVariable underlyingTypeVar =
                (TypeVariable)variable.getUnderlyingType();
            underlyingTypeTree =
                maker.Ident((Symbol.TypeSymbol)(underlyingTypeVar).asElement());
            break;
        }
        case WILDCARD: {
            AnnotatedTypeMirror.AnnotatedWildcardType wildcard =
                (AnnotatedTypeMirror.AnnotatedWildcardType) annotatedType;
            if (wildcard.getExtendsBound() != null) {
                Tree annotatedExtendsBound = AnnotatedType(wildcard.getExtendsBound());
                underlyingTypeTree =
                    maker.Wildcard(maker.TypeBoundKind(BoundKind.EXTENDS),
                                   (JCTree)annotatedExtendsBound);
            } else if (wildcard.getSuperBound() != null) {
                Tree annotatedSuperBound = AnnotatedType(wildcard.getSuperBound());
                underlyingTypeTree =
                    maker.Wildcard(maker.TypeBoundKind(BoundKind.SUPER),
                                   (JCTree)annotatedSuperBound);
            } else {
                underlyingTypeTree =
                    maker.Wildcard(maker.TypeBoundKind(BoundKind.UNBOUND),
                                   maker.TypeIdent(TypeTags.VOID));
            }
            break;
        }
        case DECLARED: {
            underlyingTypeTree = maker.Type((Type)annotatedType.getUnderlyingType());

            if (underlyingTypeTree instanceof JCTree.JCTypeApply) {
                // Replace the type parameters with annotated versions.
                AnnotatedTypeMirror.AnnotatedDeclaredType annotatedDeclaredType =
                    (AnnotatedTypeMirror.AnnotatedDeclaredType)annotatedType;
                List<JCTree.JCExpression> typeArgTrees = List.nil();
                for (AnnotatedTypeMirror arg : annotatedDeclaredType.getTypeArguments()) {
                    typeArgTrees =
                        typeArgTrees.append((JCTree.JCExpression)AnnotatedType(arg));
                }
                JCTree.JCExpression clazz =
                    (JCTree.JCExpression) ((JCTree.JCTypeApply)underlyingTypeTree).getType();
                underlyingTypeTree = maker.TypeApply(clazz, typeArgTrees);
            }
            break;
        }
        case ARRAY: {
            AnnotatedTypeMirror.AnnotatedArrayType annotatedArrayType =
                (AnnotatedTypeMirror.AnnotatedArrayType)annotatedType;
            Tree annotatedComponentTree =
                AnnotatedType(annotatedArrayType.getComponentType());
            underlyingTypeTree =
                maker.TypeArray((JCTree.JCExpression)annotatedComponentTree);
            break;
        }
        case ERROR:
            underlyingTypeTree = maker.TypeIdent(TypeTags.ERROR);
            break;
        default:
            assert false : "unexpected type: " + annotatedType;
            underlyingTypeTree = null;
            break;
        }

        ((JCTree)underlyingTypeTree).setType((Type)annotatedType.getUnderlyingType());
        JCTree.JCAnnotatedType annotatedTypeTree =
            maker.AnnotatedType(annotationTrees,
                                (JCTree.JCExpression)underlyingTypeTree);
        annotatedTypeTree.setType((Type)annotatedType.getUnderlyingType());

        return annotatedTypeTree;
    }

    /**
     * Returns a newly created Attribute.TypeCompound corresponding to an
     * argument AnnotationMirror.
     *
     * @param am  an AnnotationMirror, which may be part of an AST or an internally
     *            created subclass.
     * @return  a new Attribute.TypeCompound corresponding to the AnnotationMirror
     */
    private Attribute.TypeCompound attributeFromAnnotationMirror(AnnotationMirror am) {
        // Create a new Attribute to match the AnnotationMirror.
        List<Pair<Symbol.MethodSymbol, Attribute>> values = List.nil();
        for (Map.Entry<? extends ExecutableElement,
                 ? extends AnnotationValue> entry :
                 am.getElementValues().entrySet()) {
            Attribute attribute = attributeFromAnnotationValue(entry.getValue());
            values = values.append(new Pair<>((Symbol.MethodSymbol)entry.getKey(),
                                              attribute));
        }
        Attribute.Compound compound =
            new Attribute.Compound((Type.ClassType)am.getAnnotationType(),
                                   values);
        return new Attribute.TypeCompound(compound, new TypeAnnotationPosition());
    }

    /**
     * Returns a newly created Attribute corresponding to an argument
     * AnnotationValue.
     *
     * @param am  an AnnotationValue, which may be part of an AST or an internally
     *            created subclass.
     * @return  a new Attribute corresponding to the AnnotationValue
     */
    private Attribute attributeFromAnnotationValue(AnnotationValue av) {
        // Create a new Attribute to match the AnnotationValue.
        if (av instanceof AnnotationMirror) {
            return attributeFromAnnotationMirror((AnnotationMirror)av);
        }

        // TODO: Create other kinds of Attributes.

        Object value = av.getValue();
        if (value instanceof Type) {
            return new Attribute.Class(javacTypes, (Type)value);
        } else if (value instanceof Symbol.VarSymbol) {
            Symbol.VarSymbol sym = (Symbol.VarSymbol) value;
            if (sym.getKind() == ElementKind.ENUM) {
                return new Attribute.Enum(sym.type, sym);
            }
        }

        assert false : "Unexpected type of AnnotationValue: " + value.getClass();
        return null;
    }
}
