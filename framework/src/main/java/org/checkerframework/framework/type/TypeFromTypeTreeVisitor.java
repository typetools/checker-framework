package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.WildcardTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Converts type trees into AnnotatedTypeMirrors.
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
class TypeFromTypeTreeVisitor extends TypeFromTreeVisitor {

    private final Map<Tree, AnnotatedTypeMirror> visitedBounds = new HashMap<>();

    @Override
    public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror type = visit(node.getUnderlyingType(), f);
        if (type == null) { // e.g., for receiver type
            type = f.toAnnotatedType(f.types.getNoType(TypeKind.NONE), false);
        }
        assert AnnotatedTypeFactory.validAnnotatedType(type);
        List<? extends AnnotationMirror> annos = TreeUtils.annotationsFromTree(node);

        if (type.getKind() == TypeKind.WILDCARD) {
            final ExpressionTree underlyingTree = node.getUnderlyingType();

            if (underlyingTree.getKind() == Kind.UNBOUNDED_WILDCARD) {
                // primary annotations on unbounded wildcard types apply to both bounds
                ((AnnotatedWildcardType) type).getExtendsBound().addMissingAnnotations(annos);
                ((AnnotatedWildcardType) type).getSuperBound().addMissingAnnotations(annos);

            } else if (underlyingTree.getKind() == Kind.EXTENDS_WILDCARD) {
                ((AnnotatedWildcardType) type).getSuperBound().addMissingAnnotations(annos);

            } else if (underlyingTree.getKind() == Kind.SUPER_WILDCARD) {
                ((AnnotatedWildcardType) type).getExtendsBound().addMissingAnnotations(annos);

            } else {
                throw new BugInCF(
                        "Unexpected kind for type.  node="
                                + node
                                + " type="
                                + type
                                + " kind="
                                + underlyingTree.getKind());
            }
        } else {
            type.addAnnotations(annos);
        }

        return type;
    }

    @Override
    public AnnotatedTypeMirror visitArrayType(ArrayTypeTree node, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror component = visit(node.getType(), f);

        AnnotatedTypeMirror result = f.type(node);
        assert result instanceof AnnotatedArrayType;
        ((AnnotatedArrayType) result).setComponentType(component);
        return result;
    }

    @Override
    public AnnotatedTypeMirror visitParameterizedType(
            ParameterizedTypeTree node, AnnotatedTypeFactory f) {

        List<AnnotatedTypeMirror> args = new ArrayList<>(node.getTypeArguments().size());
        for (Tree t : node.getTypeArguments()) {
            args.add(visit(t, f));
        }

        AnnotatedTypeMirror result = f.type(node); // use creator?
        AnnotatedTypeMirror atype = visit(node.getType(), f);
        result.addAnnotations(atype.getAnnotations());
        // new ArrayList<>() type is AnnotatedExecutableType for some reason

        if (result instanceof AnnotatedDeclaredType) {
            assert result instanceof AnnotatedDeclaredType : node + " --> " + result;
            ((AnnotatedDeclaredType) result).setTypeArguments(args);
        }
        return result;
    }

    @Override
    public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeFactory f) {
        return f.type(node);
    }

    @Override
    public AnnotatedTypeMirror visitTypeParameter(TypeParameterTree node, AnnotatedTypeFactory f) {

        List<AnnotatedTypeMirror> bounds = new ArrayList<>(node.getBounds().size());
        for (Tree t : node.getBounds()) {
            AnnotatedTypeMirror bound;
            if (visitedBounds.containsKey(t) && f == visitedBounds.get(t).atypeFactory) {
                bound = visitedBounds.get(t);
            } else {
                visitedBounds.put(t, f.type(t));
                bound = visit(t, f);
                visitedBounds.remove(t);
            }
            bounds.add(bound);
        }

        AnnotatedTypeVariable result = (AnnotatedTypeVariable) f.type(node);
        List<? extends AnnotationMirror> annotations = TreeUtils.annotationsFromTree(node);
        result.getLowerBound().addAnnotations(annotations);

        switch (bounds.size()) {
            case 0:
                break;
            case 1:
                result.setUpperBound(bounds.get(0));
                break;
            default:
                AnnotatedIntersectionType upperBound =
                        (AnnotatedIntersectionType) result.getUpperBound();

                List<AnnotatedDeclaredType> superBounds = new ArrayList<>(bounds.size());
                for (AnnotatedTypeMirror b : bounds) {
                    superBounds.add((AnnotatedDeclaredType) b);
                }
                upperBound.setDirectSuperTypes(superBounds);
        }

        return result;
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(WildcardTree node, AnnotatedTypeFactory f) {

        AnnotatedTypeMirror bound = visit(node.getBound(), f);

        AnnotatedTypeMirror result = f.type(node);
        assert result instanceof AnnotatedWildcardType;

        // for wildcards unlike type variables there are bounds that differ in type from
        // result.  These occur for RAW types.  In this case, use the newly created bound
        // rather than merging into result
        if (node.getKind() == Tree.Kind.SUPER_WILDCARD) {
            ((AnnotatedWildcardType) result).setSuperBound(bound);

        } else if (node.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
            ((AnnotatedWildcardType) result).setExtendsBound(bound);
        }
        return result;
    }

    private AnnotatedTypeMirror forTypeVariable(AnnotatedTypeMirror type, AnnotatedTypeFactory f) {
        if (type.getKind() != TypeKind.TYPEVAR) {
            throw new BugInCF(
                    "TypeFromTree.forTypeVariable: should only be called on type variables");
        }

        TypeVariable typeVar = (TypeVariable) type.getUnderlyingType();
        TypeParameterElement tpe = (TypeParameterElement) typeVar.asElement();
        Element elt = tpe.getGenericElement();
        if (elt instanceof TypeElement) {
            TypeElement typeElt = (TypeElement) elt;
            int idx = typeElt.getTypeParameters().indexOf(tpe);
            ClassTree cls = (ClassTree) f.declarationFromElement(typeElt);
            if (cls != null) {
                // `forTypeVariable` is called for Identifier, MemberSelect and UnionType trees,
                // none of which are declarations.  But `cls.getTypeParameters()` returns a list
                // of type parameter declarations (`TypeParameterTree`), so this recursive call
                // to `visit` will return a declaration ATV.  So we must copy the result and set
                // its `isDeclaration` field to `false`.
                AnnotatedTypeMirror result =
                        visit(cls.getTypeParameters().get(idx), f).shallowCopy();
                ((AnnotatedTypeVariable) result).setDeclaration(false);
                return result;
            } else {
                // We already have all info from the element -> nothing to do.
                return type;
            }
        } else if (elt instanceof ExecutableElement) {
            ExecutableElement exElt = (ExecutableElement) elt;
            int idx = exElt.getTypeParameters().indexOf(tpe);
            MethodTree meth = (MethodTree) f.declarationFromElement(exElt);
            if (meth != null) {
                // This works the same as the case above.  Even though `meth` itself is not a
                // type declaration tree, the elements of `meth.getTypeParameters()` still are.
                AnnotatedTypeMirror result =
                        visit(meth.getTypeParameters().get(idx), f).shallowCopy();
                ((AnnotatedTypeVariable) result).setDeclaration(false);
                return result;
            } else {
                // throw new BugInCF("TypeFromTree.forTypeVariable: did not find source for: "
                //                   + elt);
                return type;
            }
        } else {
            // Captured types can have a generic element (owner) that is
            // not an element at all, namely Symtab.noSymbol.
            if (TypesUtils.isCaptured(typeVar)) {
                return type;
            } else {
                throw new BugInCF("TypeFromTree.forTypeVariable: not a supported element: " + elt);
            }
        }
    }

    @Override
    public AnnotatedTypeMirror visitIdentifier(IdentifierTree node, AnnotatedTypeFactory f) {

        AnnotatedTypeMirror type = f.type(node);

        if (type.getKind() == TypeKind.TYPEVAR) {
            return forTypeVariable(type, f).asUse();
        }

        return type;
    }

    @Override
    public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree node, AnnotatedTypeFactory f) {

        AnnotatedTypeMirror type = f.type(node);

        if (type.getKind() == TypeKind.TYPEVAR) {
            return forTypeVariable(type, f).asUse();
        }

        return type;
    }

    @Override
    public AnnotatedTypeMirror visitUnionType(UnionTypeTree node, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror type = f.type(node);

        if (type.getKind() == TypeKind.TYPEVAR) {
            return forTypeVariable(type, f).asUse();
        }

        return type;
    }

    @Override
    public AnnotatedTypeMirror visitIntersectionType(
            IntersectionTypeTree node, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror type = f.type(node);

        if (type.getKind() == TypeKind.TYPEVAR) {
            return forTypeVariable(type, f).asUse();
        }

        return type;
    }
}
