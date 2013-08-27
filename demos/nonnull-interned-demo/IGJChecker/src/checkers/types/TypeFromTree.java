package checkers.types;

import java.util.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.AnnotatedTypes;
import checkers.util.ElementUtils;
import checkers.util.TypesUtils;

import com.sun.source.tree.*;
import com.sun.source.util.*;

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
        if (node == null)
            throw new IllegalArgumentException("null tree");
        throw new UnsupportedOperationException(
                "conversion undefined for tree type " + node.getKind());
    }
    
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
    static class TypeFromExpression extends TypeFromTree {

        /** The singleton instance. */
        public static final TypeFromExpression INSTANCE
            = new TypeFromExpression();
        
        private TypeFromExpression() {}

        @Override
        public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node,
                AnnotatedTypeFactory f) {
            return f.fromTypeTree(node);
        }
        
        @Override
        public AnnotatedTypeMirror visitArrayAccess(ArrayAccessTree node,
                AnnotatedTypeFactory f) {

            // TODO check multidimensional array accesses
            
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
            
            AnnotatedTypes annoTypes = f.atypes;
            
            AnnotatedTypeMirror trueType 
                = f.getAnnotatedType(node.getTrueExpression());
            AnnotatedTypeMirror falseType 
                = f.getAnnotatedType(node.getFalseExpression());

            if (trueType.equals(falseType))
                return trueType;
            
            // If one of them is null, return the other
            if (trueType.getKind() == TypeKind.NULL)
                return falseType;
            else if (falseType.getKind() == TypeKind.NULL)
                return trueType;
            
            AnnotatedTypeMirror alub = f.type(node);
            TypeMirror lub = alub.getUnderlyingType();
            
//            f.atypes.annotateAsLub(alub, trueType, falseType);
//            return alub;
            // It it anonoymous
            if (TypesUtils.isAnonymousType(lub)) {
                // Find the intersect types
                f.atypes.annotateAsLub(alub, trueType, falseType);
            } else {
                trueType = annoTypes.asSuper(trueType, alub);
                falseType = annoTypes.asSuper(falseType, alub);
                
                if (trueType.equals(falseType))
                    return trueType;
                // Errr... type variables don't work yet, and asSuper returns null
                // This needs to go
                if (trueType == null && falseType == null) return alub;
                else if (trueType == null) return falseType;
                else if (falseType == null) return trueType;
                
                f.atypes.annotateAsLub(alub, trueType, falseType);
            }
            return alub;
        }
                
        @Override
        public AnnotatedTypeMirror visitIdentifier(IdentifierTree node,
                AnnotatedTypeFactory f) {

            Element elt = f.elementFromUse(node);
            if (node.getName().contentEquals("this") 
                    && elt.getKind() != ElementKind.CONSTRUCTOR)
                return f.getSelfType(node);
            Tree decl = f.declarationFromElement(elt);

            if (decl instanceof ClassTree)
                return f.fromClass((ClassTree)decl);
            else if (decl instanceof MethodTree)
                return f.fromMember(decl);
            else if (decl instanceof VariableTree)
                return f.fromMember(decl);
            else return f.fromElement(elt);
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
               
            Element elt = f.elementFromUse(node);
            if (elt.getKind().isClass() || elt.getKind().isInterface())
                return f.getAnnotatedType(elt);

            // The expression might be a primitive type (as in "int.class").
            if (!(node.getExpression() instanceof PrimitiveTypeTree)) {
                // We need the original t with the implicit annotations
                AnnotatedTypeMirror t = f.getAnnotatedType(node.getExpression());
                if (t instanceof AnnotatedDeclaredType)
                    return f.atypes.asMemberOf(t, elt);
            }

            Tree decl = f.declarationFromElement(elt);
            if (decl instanceof ClassTree)
                return f.fromClass((ClassTree)decl);
            else if (decl instanceof MethodTree)
                return f.fromMember(decl);
            else if (decl instanceof VariableTree)
                return f.fromMember(decl);
            else return f.fromElement(elt);
        }

        @Override
        public AnnotatedTypeMirror visitMethodInvocation(
                MethodInvocationTree node, AnnotatedTypeFactory f) {

            AnnotatedExecutableType ex = f.methodFromUse(node);
            return ex.getReturnType();
        }

        @Override
        public AnnotatedTypeMirror visitNewArray(NewArrayTree node,
                AnnotatedTypeFactory f) {

            // Don't use fromTypeTree here, because node.getType() is not an
            // array type!
            AnnotatedTypeMirror result = f.type(node);
            assert result instanceof AnnotatedArrayType;

            if (node.getType() == null) // e.g., byte[] b = {(byte)1, (byte)2};
                return result;
           
            // Copy annotations from the type.
            AnnotatedTypeMirror treeElem = f.fromTypeTree(node.getType());
            AnnotatedTypeMirror typeElem = 
                ((AnnotatedArrayType)result).getComponentType();
            while (true) {
                typeElem.addAnnotations(treeElem.getAnnotations());
                if (!(treeElem instanceof AnnotatedArrayType)) break;
                assert typeElem instanceof AnnotatedArrayType;
                treeElem = ((AnnotatedArrayType)treeElem).getComponentType();
                typeElem = ((AnnotatedArrayType)typeElem).getComponentType();
            }
            
            // Add top-level annotations.
            result.addAnnotations(f.annotationsFromArrayCreation(node, -1));
                        
            // Add all dimension annotations.
            int idx = 0;
            AnnotatedTypeMirror level = result;
            while (level.getKind() == TypeKind.ARRAY) {
                AnnotatedArrayType array = (AnnotatedArrayType)level;
                array.getComponentType().addAnnotations(
                        f.annotationsFromArrayCreation(node, idx++));
                level = array.getComponentType();
            }

            return result;
        }

        @Override
        public AnnotatedTypeMirror visitNewClass(NewClassTree node,
                AnnotatedTypeFactory f) {
            
            // Use the annotated type of the part between "new" and the
            // constructor arguments.
            return f.fromTypeTree(node.getIdentifier());
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
            return f.type(node);
        }
    }
    
    /**
     * A singleton that obtains the annotated type of a method or variable from
     * its declaration.
     * 
     * @see AnnotatedTypeFactory#fromMember(Tree)
     */
    static class TypeFromMember extends TypeFromTree {
        
        /** The singleton instance. */
        public static final TypeFromMember INSTANCE
            = new TypeFromMember();
        
        private TypeFromMember() {}
        
        @Override
        public AnnotatedTypeMirror visitVariable(VariableTree node,
                AnnotatedTypeFactory f) {

            AnnotatedTypeMirror result = f.fromTypeTree(node.getType());

            Element elt = f.elementFromDeclaration(node);
            result.setElement(elt);

            result.addAnnotations(elt.getAnnotationMirrors());

            // If it's the last param in a varargs method, move its annotations
            // down one level.
            //
            // N.B.: There are workarounds for two bugs in javac here:
            //
            // 1. exception parameters have kind PARAMETER instead of
            // EXCEPTION_PARAMETER
            //
            // 2. ExecutableElement#getParameters() crashes in javac for a
            // static initializer (instead of returning an empty list)
            //
            if (elt.getKind() == ElementKind.PARAMETER) {
                final TreePath path = TreePath.getPath(f.root, node);
                if (path != null 
                    && path.getParentPath().getLeaf().getKind() 
                        != Tree.Kind.CATCH) {
                    Element enclosing = elt.getEnclosingElement();
                    assert enclosing instanceof ExecutableElement;
                    ExecutableElement method = (ExecutableElement)enclosing;
                    int numParams = method.getParameters().size() - 1;
                    if (method.isVarArgs() && 
                            elt.equals(method.getParameters().get(numParams))) {
                        assert result instanceof AnnotatedArrayType;
                        Set<AnnotationMirror> annos = result.getAnnotations();
                        ((AnnotatedArrayType)result).getComponentType().addAnnotations(annos);
                        for (AnnotationMirror a : annos)
                            result.removeAnnotation(a);
                    }
                }
            }
            return result;
        }

        @Override
        public AnnotatedTypeMirror visitMethod(MethodTree node,
                AnnotatedTypeFactory f) {

            ExecutableElement elt = f.elementFromDeclaration(node);

            AnnotatedExecutableType result = 
                (AnnotatedExecutableType)f.toAnnotatedType(elt.asType());
            result.setElement(elt);

            // Annotate the parameter types.
            List<AnnotatedTypeMirror> paramTypes 
                = new LinkedList<AnnotatedTypeMirror>();
            for (Tree t : node.getParameters())
                paramTypes.add(visit(t, f));
            result.setParameterTypes(paramTypes);

            // Annotate the return type.
            if (node.getReturnType() == null)
                result.setReturnType(f.toAnnotatedType(f.types.getNoType(TypeKind.VOID)));
            else
                result.setReturnType(f.fromTypeTree(node.getReturnType()));
            result.getReturnType().addAnnotations(elt.getAnnotationMirrors());

            // Annotate the receiver.
            AnnotatedTypeMirror receiver = f.fromTypeTree(null);
            assert receiver.getKind() == TypeKind.NONE;

            if (ElementUtils.isStatic(elt))
                // TODO maybe it should be TypeKind.NONE
                result.setReceiverType(null);
            else {
                AnnotatedDeclaredType enclosing;
                {
                    Element enclElt = ElementUtils.enclosingClass(elt);
                    AnnotatedTypeMirror t = f.fromElement(enclElt);
                    assert t instanceof AnnotatedDeclaredType : t;
                    enclosing = (AnnotatedDeclaredType)t;
                }
                enclosing.clearAnnotations();
                enclosing.addAnnotations(receiver.getAnnotations());
                result.setReceiverType(enclosing);
            }

            // Annotate the type parameters.
            List<AnnotatedTypeVariable> typeParams 
                = new LinkedList<AnnotatedTypeVariable>();
            for (Tree t : node.getTypeParameters()) {
                AnnotatedTypeMirror type = f.fromTypeTree(t);
                assert type instanceof AnnotatedTypeVariable;
                typeParams.add((AnnotatedTypeVariable)type);
            }
            result.setTypeVariables(typeParams);

            // Annotate throws types.
            List<AnnotatedTypeMirror> throwsTypes 
                = new LinkedList<AnnotatedTypeMirror>();
            for (Tree t : node.getThrows())
                throwsTypes.add(f.fromTypeTree(t));
            result.setThrownTypes(throwsTypes);

            return result;
        }
    }
    
    /**
     * A singleton that obtains the annotated type of a class from its
     * declaration.
     * 
     * @see AnnotatedTypeFactory#fromClass(ClassTree)
     */
    static class TypeFromClass extends TypeFromTree {
        
        /** The singleton instance. */
        public static final TypeFromClass INSTANCE
            = new TypeFromClass();
        
        private TypeFromClass() {}
        
        @Override
        public AnnotatedTypeMirror visitClass(ClassTree node,
                AnnotatedTypeFactory f) {

            TypeElement elt = f.elementFromDeclaration(node);
            AnnotatedTypeMirror result = f.toAnnotatedType(elt.asType());
            result.setElement(elt);
            result.addAnnotations(elt.getAnnotationMirrors());

            assert result instanceof AnnotatedDeclaredType;
            AnnotatedDeclaredType dt = (AnnotatedDeclaredType)result;
            
            // Get annotations on type parameters.
            List<AnnotatedTypeMirror> params = new LinkedList<AnnotatedTypeMirror>();
            for (Tree tree : node.getTypeParameters()) {
                params.add(f.fromTypeTree(tree));
            }
            dt.setTypeArguments(params);

            // TODO extends/implements

            return result;
        }
    }
    
    /**
     * A singleton that obtains the annotated type of a type in tree form.
     * 
     * @see AnnotatedTypeFactory#fromTypeTree(Tree)
     */
    static class TypeFromTypeTree extends TypeFromTree {

        /** The singleton instance. */
        public static final TypeFromTypeTree INSTANCE
            = new TypeFromTypeTree();
        
        private TypeFromTypeTree() {}
        
        private Map<Tree, AnnotatedTypeMirror> visitedBounds 
            = new HashMap<Tree, AnnotatedTypeMirror>();

        @Override
        public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node,
                AnnotatedTypeFactory f) {
            AnnotatedTypeMirror type = visit(node.getUnderlyingType(), f);
            if (type == null) // e.g., for receiver type
                type = f.toAnnotatedType(f.types.getNoType(TypeKind.NONE));
            assert AnnotatedTypeFactory.validAnnotatedType(type);
            type.addAnnotations(f.annotationsFromTree(node));
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
            for (Tree t : node.getTypeArguments())
                args.add(visit(t, f));

            AnnotatedTypeMirror result = f.type(node); // use creator?
            AnnotatedTypeMirror atype = f.fromTypeTree(node.getType());
            result.addAnnotations(atype.getAnnotations());
            assert result instanceof AnnotatedDeclaredType : node + " --> " + result;
            ((AnnotatedDeclaredType)result).setTypeArguments(args);
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

            List<AnnotatedTypeMirror> bounds
                = new LinkedList<AnnotatedTypeMirror>();
            for (Tree t : node.getBounds()) {
                AnnotatedTypeMirror bound;
                if (visitedBounds.containsKey(t))
                    bound = visitedBounds.get(t);
                else {
                    visitedBounds.put(t, f.type(t));
                    bound = visit(t, f);
                    visitedBounds.put(t, bound);
                }
                bounds.add(bound);
            }

            if (bounds.size() > 1)
                throw new UnsupportedOperationException(
                        "intersection types are not currently supported");

            AnnotatedTypeMirror result = f.type(node);
            assert result instanceof AnnotatedTypeVariable;
            if (!bounds.isEmpty())
                ((AnnotatedTypeVariable)result).setUpperBound(bounds.get(0));

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
            if (type.getKind() != TypeKind.TYPEVAR)
                throw new IllegalArgumentException();

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
            } else throw new AssertionError();
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
    }
}