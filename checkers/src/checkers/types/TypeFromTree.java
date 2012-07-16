package checkers.types;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;

import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.types.AnnotatedTypeMirror.AnnotatedTypeVariable;
import checkers.types.AnnotatedTypeMirror.AnnotatedWildcardType;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;
import checkers.util.TypesUtils;

import com.sun.source.tree.*;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * A utility class used to abstract common functionality from tree-to-type
 * converters. By default, when visiting a tree for which a visitor method has
 * not explicitly been provided, the visitor will throw an
 * {@link UnsupportedOperationException}; when visiting a null tree, it will
 * throw an {@link IllegalArgumentException}.
 */
abstract class TypeFromTree extends
        SimpleTreeVisitor<AnnotatedTypeMirror, AnnotatedTypeFactory> {

    @Override
    public AnnotatedTypeMirror defaultAction(Tree node, AnnotatedTypeFactory f) {
        if (node == null) {
            SourceChecker.errorAbort("TypeFromTree.defaultAction: null tree");
            return null; // dead code
        }
        SourceChecker.errorAbort("TypeFromTree.defaultAction: conversion undefined for tree type " + node.getKind());
        return null; // dead code
    }

    /** The singleton TypeFromExpression instance. */
    public static final TypeFromExpression TypeFromExpressionINSTANCE
        = new TypeFromExpression();

    /**
     * A singleton that obtains the annotated type of an {@link ExpressionTree}.
     *
     * <p>
     *
     * All subtypes of {@link ExpressionTree} are supported, except:
     * <ul>
     *  <li>{@link AnnotationTree}</li>
     *  <li>{@link ErroneousTree}</li>
     * </ul>
     *
     * @see AnnotatedTypeFactory#fromExpression(ExpressionTree)
     */
    private static class TypeFromExpression extends TypeFromTree {

        private TypeFromExpression() {}

        @Override
        public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node,
                AnnotatedTypeFactory f) {
            return f.fromTypeTree(node);
        }

        @Override
        public AnnotatedTypeMirror visitArrayAccess(ArrayAccessTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror type = f.getAnnotatedType(node.getExpression());
            assert type instanceof AnnotatedArrayType;
            return ((AnnotatedArrayType)type).getComponentType();
        }

        @Override
        public AnnotatedTypeMirror visitAssignment(AssignmentTree node,
                AnnotatedTypeFactory f) {

            // Recurse on the type of the variable.
            return visit(node.getVariable(), f);
        }

        @Override
        public AnnotatedTypeMirror visitBinary(BinaryTree node,
                AnnotatedTypeFactory f) {
            return f.type(node);
        }

        @Override
        public AnnotatedTypeMirror visitCompoundAssignment(
                CompoundAssignmentTree node, AnnotatedTypeFactory f) {

            // Recurse on the type of the variable.
            return visit(node.getVariable(), f);
        }

        @Override
        public AnnotatedTypeMirror visitConditionalExpression(
                ConditionalExpressionTree node, AnnotatedTypeFactory f) {

            AnnotatedTypeMirror trueType = f.getAnnotatedType(node.getTrueExpression());
            AnnotatedTypeMirror falseType = f.getAnnotatedType(node.getFalseExpression());

            if (trueType.equals(falseType))
                return trueType;

            AnnotatedTypeMirror alub = f.type(node);
            trueType = f.atypes.asSuper(trueType, alub);
            falseType = f.atypes.asSuper(falseType, alub);

            if (trueType!=null && trueType.equals(falseType))
                return trueType;

            List<AnnotatedTypeMirror> types = new ArrayList<AnnotatedTypeMirror>();
            types.add(trueType);
            types.add(falseType);
            f.atypes.annotateAsLub(alub, types);

            return alub;
        }

        @Override
        public AnnotatedTypeMirror visitIdentifier(IdentifierTree node,
                AnnotatedTypeFactory f) {
            if (node.getName().contentEquals("this")
                    || node.getName().contentEquals("super")) {
                AnnotatedDeclaredType res = f.getSelfType(node);
                return res;
            }

            Element elt = TreeUtils.elementFromUse(node);
            AnnotatedTypeMirror selfType = f.getImplicitReceiverType(node);
            if (selfType != null) {
                return f.atypes.asMemberOf(selfType, elt);
            }

            return f.getAnnotatedType(elt);
        }

        @Override
        public AnnotatedTypeMirror visitInstanceOf(InstanceOfTree node,
                AnnotatedTypeFactory f) {
            return f.type(node);
        }

        @Override
        public AnnotatedTypeMirror visitLiteral(LiteralTree node,
                AnnotatedTypeFactory f) {
            return f.type(node);
        }

        @Override
        public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree node,
                AnnotatedTypeFactory f) {

            Element elt = TreeUtils.elementFromUse(node);
            if (elt.getKind().isClass() || elt.getKind().isInterface())
                return f.fromElement(elt);

            // The expression might be a primitive type (as in "int.class").
            if (!(node.getExpression() instanceof PrimitiveTypeTree)) {
                // TODO: why don't we use getSelfType here?
                if (node.getIdentifier().contentEquals("this")) {
                    return f.getEnclosingType((TypeElement)InternalUtils.symbol(node.getExpression()), node);
                }
                // We need the original t with the implicit annotations
                AnnotatedTypeMirror t = f.getAnnotatedType(node.getExpression());
                if (t instanceof AnnotatedDeclaredType)
                    return f.atypes.asMemberOf(t, elt);
            }

            return f.fromElement(elt);
        }

        @Override
        public AnnotatedTypeMirror visitMethodInvocation(
                MethodInvocationTree node, AnnotatedTypeFactory f) {

            AnnotatedExecutableType ex = f.methodFromUse(node).first;
            return ex.getReturnType();
        }

        @Override
        public AnnotatedTypeMirror visitNewArray(NewArrayTree node,
                AnnotatedTypeFactory f) {

            // Don't use fromTypeTree here, because node.getType() is not an
            // array type!
            AnnotatedArrayType result = (AnnotatedArrayType)f.type(node);

            if (node.getType() == null) // e.g., byte[] b = {(byte)1, (byte)2};
                return result;

            annotateArrayAsArray(result, node, f);

            return result;
        }

        private AnnotatedTypeMirror descendBy(AnnotatedTypeMirror type, int depth) {
            AnnotatedTypeMirror result = type;
            while (depth > 0) {
                result = ((AnnotatedArrayType)result).getComponentType();
                depth--;
            }
            return result;
        }

        private void annotateArrayAsArray(AnnotatedArrayType result, NewArrayTree node, AnnotatedTypeFactory f) {
            // Copy annotations from the type.
            AnnotatedTypeMirror treeElem = f.fromTypeTree(node.getType());
            boolean hasInit = node.getInitializers() != null;
            AnnotatedTypeMirror typeElem = descendBy(result,
                    hasInit ? 1 : node.getDimensions().size());
            while (true) {
                typeElem.addAnnotations(treeElem.getAnnotations());
                if (!(treeElem instanceof AnnotatedArrayType)) break;
                assert typeElem instanceof AnnotatedArrayType;
                treeElem = ((AnnotatedArrayType)treeElem).getComponentType();
                typeElem = ((AnnotatedArrayType)typeElem).getComponentType();
            }
            // Add all dimension annotations.
            int idx = 0;
            AnnotatedTypeMirror level = result;
            while (level.getKind() == TypeKind.ARRAY) {
                AnnotatedArrayType array = (AnnotatedArrayType)level;
                List<? extends AnnotationMirror> annos = InternalUtils.annotationsFromArrayCreation(node, idx++);
                array.addAnnotations(annos);
                level = array.getComponentType();
            }

            // Add top-level annotations.
            result.addAnnotations(InternalUtils.annotationsFromArrayCreation(node, -1));
        }

        @Override
        public AnnotatedTypeMirror visitNewClass(NewClassTree node,
                AnnotatedTypeFactory f) {

            // Use the annotated type of the part between "new" and the
            // constructor arguments.
            AnnotatedDeclaredType type = f.fromNewClass(node);
            if (node.getClassBody() != null) {
                DeclaredType dt = (DeclaredType)InternalUtils.typeOf(node);
                AnnotatedDeclaredType anonType =
                    (AnnotatedDeclaredType)AnnotatedTypeMirror.createType(dt, f.env, f);
                anonType.setElement(type.getElement());
                if (type.isAnnotated()) {
                    List<AnnotatedDeclaredType> supertypes = Collections.singletonList(type);
                    anonType.setDirectSuperTypes(supertypes);
                    anonType.addAnnotations(type.getAnnotations());
                    f.postDirectSuperTypes(anonType, supertypes);
                }
                type = anonType;
            }
            return type;
        }

        @Override
        public AnnotatedTypeMirror visitParenthesized(ParenthesizedTree node,
                AnnotatedTypeFactory f) {

            // Recurse on the expression inside the parens.
            return visit(node.getExpression(), f);
        }

        @Override
        public AnnotatedTypeMirror visitTypeCast(TypeCastTree node,
                AnnotatedTypeFactory f) {

            // Use the annotated type of the type in the cast.
            return f.fromTypeTree(node.getType());
        }

        @Override
        public AnnotatedTypeMirror visitUnary(UnaryTree node,
                AnnotatedTypeFactory f) {
            // TODO: why not visit(node.getExpression(), f)
            return f.type(node);
        }

        @Override
        public AnnotatedTypeMirror visitWildcard(WildcardTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror bound = visit(node.getBound(), f);

            AnnotatedTypeMirror result = f.type(node);
            assert result instanceof AnnotatedWildcardType;
            if (node.getKind() == Tree.Kind.SUPER_WILDCARD)
                ((AnnotatedWildcardType)result).setSuperBound(bound);
            else if (node.getKind() == Tree.Kind.EXTENDS_WILDCARD)
                ((AnnotatedWildcardType)result).setExtendsBound(bound);
            return result;
        }

        @Override
        public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree node,
                AnnotatedTypeFactory f) {
            // for e.g. "int.class"
            return f.fromTypeTree(node);
        }

        @Override
        public AnnotatedTypeMirror visitArrayType(ArrayTypeTree node,
                AnnotatedTypeFactory f) {
            // for e.g. "int[].class"
            return f.fromTypeTree(node);
        }

        @Override
        public AnnotatedTypeMirror visitParameterizedType(ParameterizedTypeTree node, AnnotatedTypeFactory f) {
            return f.fromTypeTree(node);
        }
    }


    /** The singleton TypeFromMember instance. */
    public static final TypeFromMember TypeFromMemberINSTANCE
        = new TypeFromMember();

    /**
     * A singleton that obtains the annotated type of a method or variable from
     * its declaration.
     *
     * @see AnnotatedTypeFactory#fromMember(Tree)
     */
    private static class TypeFromMember extends TypeFromTree {

        private TypeFromMember() {}

        @Override
        public AnnotatedTypeMirror visitVariable(VariableTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror result = f.fromTypeTree(node.getType());
            // I would like to use this line: 
            // AnnotatedTypeMirror result = f.toAnnotatedType(elt.asType());
            // Instead of the above, but the typeAnnotations are not filled into
            // the VarSymbol of a local variable :-(
            // TODO: fix this!
            Element elt = TreeUtils.elementFromDeclaration(node);
            result.setElement(elt);

            TypeFromElement.annotate(result, elt);
            return result;
        }

        @Override
        public AnnotatedTypeMirror visitMethod(MethodTree node,
                AnnotatedTypeFactory f) {

            ExecutableElement elt = TreeUtils.elementFromDeclaration(node);

            AnnotatedExecutableType result =
                (AnnotatedExecutableType)f.toAnnotatedType(elt.asType());
            result.setElement(elt);

            TypeFromElement.annotate(result, elt);

            return result;
        }
    }


    /** The singleton TypeFromClass instance. */
    public static final TypeFromClass TypeFromClassINSTANCE
        = new TypeFromClass();

    /**
     * A singleton that obtains the annotated type of a class from its
     * declaration.
     *
     * @see AnnotatedTypeFactory#fromClass(ClassTree)
     */
    private static class TypeFromClass extends TypeFromTree {

        private TypeFromClass() {}

        @Override
        public AnnotatedTypeMirror visitClass(ClassTree node,
                AnnotatedTypeFactory f) {
            TypeElement elt = TreeUtils.elementFromDeclaration(node);
            AnnotatedTypeMirror result = f.toAnnotatedType(elt.asType());
            result.setElement(elt);

            TypeFromElement.annotate(result, elt);

            return result;
        }
    }


    /** The singleton TypeFromTypeTree instance. */
    public static final TypeFromTypeTree TypeFromTypeTreeINSTANCE
        = new TypeFromTypeTree();

    /**
     * A singleton that obtains the annotated type of a type in tree form.
     *
     * @see AnnotatedTypeFactory#fromTypeTree(Tree)
     */
    private static class TypeFromTypeTree extends TypeFromTree {

        private TypeFromTypeTree() {}

        private final Map<Tree, AnnotatedTypeMirror> visitedBounds
            = new HashMap<Tree, AnnotatedTypeMirror>();

        @Override
        public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node,
                AnnotatedTypeFactory f) {
            AnnotatedTypeMirror type = visit(node.getUnderlyingType(), f);
            if (type == null) // e.g., for receiver type
                type = f.toAnnotatedType(f.types.getNoType(TypeKind.NONE));
            assert AnnotatedTypeFactory.validAnnotatedType(type);
            List<? extends AnnotationMirror> annos = InternalUtils.annotationsFromTree(node);
            type.addAnnotations(annos);

            if (type.getKind() == TypeKind.TYPEVAR &&
                    !((AnnotatedTypeVariable)type).getUpperBound().isAnnotated()) {
                ((AnnotatedTypeVariable)type).getUpperBound().addAnnotations(annos);
            }

            if (type.getKind() == TypeKind.WILDCARD &&
                    !((AnnotatedWildcardType)type).getExtendsBound().isAnnotated()) {
                ((AnnotatedWildcardType)type).getExtendsBound().addAnnotations(annos);
            }

            return type;
        }

        @Override
        public AnnotatedTypeMirror visitArrayType(ArrayTypeTree node,
                AnnotatedTypeFactory f) {
            AnnotatedTypeMirror component = visit(node.getType(), f);

            AnnotatedTypeMirror result = f.type(node);
            assert result instanceof AnnotatedArrayType;
            ((AnnotatedArrayType)result).setComponentType(component);
            return result;
        }

        @Override
        public AnnotatedTypeMirror visitParameterizedType(
                ParameterizedTypeTree node, AnnotatedTypeFactory f) {

            List<AnnotatedTypeMirror> args = new LinkedList<AnnotatedTypeMirror>();
            for (Tree t : node.getTypeArguments()) {
                args.add(visit(t, f));
            }

            AnnotatedTypeMirror result = f.type(node); // use creator?
            AnnotatedTypeMirror atype = visit(node.getType(), f);
            result.addAnnotations(atype.getAnnotations());
            // new ArrayList<>() type is AnnotatedExecutableType for some reason

            if (result instanceof AnnotatedDeclaredType) {
                assert result instanceof AnnotatedDeclaredType : node + " --> " + result;
                ((AnnotatedDeclaredType)result).setTypeArguments(args);
            }
            return result;
        }

        @Override
        public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree node,
                AnnotatedTypeFactory f) {
            return f.type(node);
        }

        @Override
        public AnnotatedTypeMirror visitTypeParameter(TypeParameterTree node,
                AnnotatedTypeFactory f) {

            List<AnnotatedTypeMirror> bounds = new LinkedList<AnnotatedTypeMirror>();
            for (Tree t : node.getBounds()) {
                AnnotatedTypeMirror bound;
                if (visitedBounds.containsKey(t) && f == visitedBounds.get(t).typeFactory) {
                    bound = visitedBounds.get(t);
                } else {
                    visitedBounds.put(t, f.type(t));
                    bound = visit(t, f);
                    visitedBounds.remove(t);
                }
                bounds.add(bound);
            }

            AnnotatedTypeVariable result = (AnnotatedTypeVariable) f.type(node);
            List<? extends AnnotationMirror> annotations = InternalUtils.annotationsFromTree(node);

            if (f.canHaveAnnotatedTypeParameters())
                result.addAnnotations(annotations);
            result.getUpperBound().addAnnotations(annotations);
            assert result instanceof AnnotatedTypeVariable;
            switch (bounds.size()) {
            case 0: break;
            case 1:
                result.setUpperBound(bounds.get(0));
                break;
            default:
                AnnotatedDeclaredType upperBound = (AnnotatedDeclaredType)result.getUpperBound();
                assert TypesUtils.isAnonymousType(upperBound.getUnderlyingType());

                List<AnnotatedDeclaredType> superBounds = new ArrayList<AnnotatedDeclaredType>();
                for (AnnotatedTypeMirror b : bounds) {
                    superBounds.add((AnnotatedDeclaredType)b);
                }
                upperBound.setDirectSuperTypes(superBounds);
            }

            return result;
        }

        @Override
        public AnnotatedTypeMirror visitWildcard(WildcardTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror bound = visit(node.getBound(), f);

            AnnotatedTypeMirror result = f.type(node);
            assert result instanceof AnnotatedWildcardType;
            if (node.getKind() == Tree.Kind.SUPER_WILDCARD)
                ((AnnotatedWildcardType)result).setSuperBound(bound);
            else if (node.getKind() == Tree.Kind.EXTENDS_WILDCARD)
                ((AnnotatedWildcardType)result).setExtendsBound(bound);
            return result;
        }

        private AnnotatedTypeMirror forTypeVariable(AnnotatedTypeMirror type,
                AnnotatedTypeFactory f) {
            if (type.getKind() != TypeKind.TYPEVAR) {
                SourceChecker.errorAbort("TypeFromTree.forTypeVariable: should only be called on type variables");
                return null; // dead code
            }

            TypeParameterElement tpe = (TypeParameterElement)
                    ((TypeVariable)type.getUnderlyingType()).asElement();
            Element elt = tpe.getGenericElement();
            if (elt instanceof TypeElement) {
                TypeElement typeElt = (TypeElement)elt;
                int idx = typeElt.getTypeParameters().indexOf(tpe);
                ClassTree cls = (ClassTree)f.declarationFromElement(typeElt);
                AnnotatedTypeMirror result = visit(cls.getTypeParameters().get(idx), f);
                return result;
            } else if (elt instanceof ExecutableElement) {
                ExecutableElement exElt = (ExecutableElement)elt;
                int idx = exElt.getTypeParameters().indexOf(tpe);
                MethodTree meth = (MethodTree)f.declarationFromElement(exElt);
                AnnotatedTypeMirror result = visit(meth.getTypeParameters().get(idx), f);
                return result;
            } else {
                SourceChecker.errorAbort("TypeFromTree.forTypeVariable: not a supported element: " + elt);
                return null; // dead code
            }
        }

        @Override
        public AnnotatedTypeMirror visitIdentifier(IdentifierTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror type = f.type(node);

            if (type.getKind() == TypeKind.TYPEVAR)
                return forTypeVariable(type, f);

            return type;
        }

        @Override
        public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror type = f.type(node);

            if (type.getKind() == TypeKind.TYPEVAR)
                return forTypeVariable(type, f);

            return type;
        }

        @Override
        public AnnotatedTypeMirror visitUnionType(UnionTypeTree node,
                AnnotatedTypeFactory f) {
            AnnotatedTypeMirror type = f.type(node);

            if (type.getKind() == TypeKind.TYPEVAR)
                return forTypeVariable(type, f);

            return type;
        }
    }
}
