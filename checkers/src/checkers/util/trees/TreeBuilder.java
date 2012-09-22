package checkers.util.trees;

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import checkers.types.AnnotatedTypeMirror;
import checkers.util.InternalUtils;
import checkers.util.TypesUtils;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;

import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotationPosition;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;

/**
 * The TreeBuilder permits the creation of new AST Trees using the
 * non-public Java compiler API TreeMaker.  Initially, it will support
 * construction of desugared Trees required by the CFGBuilder, e.g.
 * the pieces of a desugared enhanced for loop.
 */

public class TreeBuilder {
    private final Elements elements;
    private final Types modelTypes;
    private final com.sun.tools.javac.code.Types javacTypes;
    private final TreeMaker maker;
    private final Names names;
    private final Symtab symtab;

    public TreeBuilder(ProcessingEnvironment env) {
        Context context = ((JavacProcessingEnvironment)env).getContext();
        elements = env.getElementUtils();
        modelTypes = env.getTypeUtils();
        javacTypes = com.sun.tools.javac.code.Types.instance(context);
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
        symtab = Symtab.instance(context);
    }

    /**
     * Builds an AST Tree to access the iterator() method of some iterable
     * expression.
     *
     * @param iterableExpr  an expression whose type is a subtype of Iterable
     * @return  a MemberSelectTree that accesses the iterator() method of
     *    the expression
     */
    public MemberSelectTree buildIteratorMethodAccess(ExpressionTree iterableExpr) {
        DeclaredType exprType =
            (DeclaredType)TypesUtils.upperBound(InternalUtils.typeOf(iterableExpr));
        assert exprType != null : "expression must be of declared type Iterable<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the iterator() method of the iterable type
        Symbol.MethodSymbol iteratorMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("iterator")) {
                    iteratorMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert iteratorMethod != null : "no iterator method declared for expression type";

        Type.MethodType methodType = (Type.MethodType)iteratorMethod.asType();
        Symbol.TypeSymbol methodClass = (Symbol.TypeSymbol)methodType.asElement();
        DeclaredType iteratorType = (DeclaredType)methodType.getReturnType();
        TypeMirror elementType;

        if (iteratorType.getTypeArguments().size() > 0) {
            elementType = iteratorType.getTypeArguments().get(0);
            // Remove captured type from a wildcard.
            if (elementType instanceof Type.CapturedType) {
                elementType = ((Type.CapturedType)elementType).wildcard;
            }

            iteratorType =
                modelTypes.getDeclaredType((TypeElement)modelTypes.asElement(iteratorType),
                                      elementType);
        }


        // Replace the iterator method's generic return type with
        // the actual element type of the expression.
        Type.MethodType updatedMethodType =
            new Type.MethodType(com.sun.tools.javac.util.List.<Type>nil(),
                                (Type)iteratorType,
                                com.sun.tools.javac.util.List.<Type>nil(),
                                methodClass);

        JCTree.JCFieldAccess iteratorAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iterableExpr,
                         iteratorMethod);
        iteratorAccess.setType(updatedMethodType);

        return iteratorAccess;
    }

    /**
     * Builds an AST Tree to access the hasNext() method of an iterator.
     *
     * @param iteratorExpr  an expression whose type is a subtype of Iterator
     * @return  a MemberSelectTree that accesses the hasNext() method of
     *    the expression
     */
    public MemberSelectTree buildHasNextMethodAccess(ExpressionTree iteratorExpr) {
        DeclaredType exprType = (DeclaredType)InternalUtils.typeOf(iteratorExpr);
        assert exprType != null : "expression must be of declared type Iterator<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the hasNext() method of the iterator type
        Symbol.MethodSymbol hasNextMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("hasNext")) {
                    hasNextMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert hasNextMethod != null : "no hasNext method declared for expression type";

        JCTree.JCFieldAccess hasNextAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iteratorExpr,
                         hasNextMethod);
        hasNextAccess.setType((Type.MethodType)hasNextMethod.asType());

        return hasNextAccess;
    }

    /**
     * Builds an AST Tree to access the next() method of an iterator.
     *
     * @param iteratorExpr  an expression whose type is a subtype of Iterator
     * @return  a MemberSelectTree that accesses the next() method of
     *    the expression
     */
    public MemberSelectTree buildNextMethodAccess(ExpressionTree iteratorExpr) {
        DeclaredType exprType = (DeclaredType)InternalUtils.typeOf(iteratorExpr);
        assert exprType != null : "expression must be of declared type Iterator<>";

        TypeElement exprElement = (TypeElement)exprType.asElement();

        // Find the next() method of the iterator type
        Symbol.MethodSymbol nextMethod = null;

        for (ExecutableElement method :
                 ElementFilter.methodsIn(elements.getAllMembers(exprElement))) {
            Name methodName = method.getSimpleName();

            if (method.getParameters().size() == 0) {
                if (methodName.contentEquals("next")) {
                    nextMethod = (Symbol.MethodSymbol)method;
                }
            }
        }

        assert nextMethod != null : "no next method declared for expression type";

        Type.MethodType methodType = (Type.MethodType)nextMethod.asType();
        Symbol.TypeSymbol methodClass = (Symbol.TypeSymbol)methodType.asElement();
        Type elementType;

        if (exprType.getTypeArguments().size() > 0) {
            elementType = (Type)exprType.getTypeArguments().get(0);
        } else {
            elementType = symtab.objectType;
        }

        // Replace the next method's generic return type with
        // the actual element type of the expression.
        Type.MethodType updatedMethodType =
            new Type.MethodType(com.sun.tools.javac.util.List.<Type>nil(),
                                elementType,
                                com.sun.tools.javac.util.List.<Type>nil(),
                                methodClass);

        JCTree.JCFieldAccess nextAccess =
            (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)iteratorExpr,
                         nextMethod);
        nextAccess.setType(updatedMethodType);

        return nextAccess;
    }

    /**
     * Builds an AST Tree to dereference the length field of an array
     *
     * @param expression  the array expression whose length is being accessed
     * @return  a MemberSelectTree to dereference the length of the array
     */
    public MemberSelectTree buildArrayLengthAccess(ExpressionTree expression) {

        return (JCTree.JCFieldAccess)
            maker.Select((JCTree.JCExpression)expression, symtab.lengthVar);
    }

    /**
     * Builds an AST Tree to call a method designated by the argument expression.
     *
     * @param methodExpr  an expression denoting a method with no arguments
     * @return  a MethodInvocationTree to call the argument method
     */
    public MethodInvocationTree buildMethodInvocation(ExpressionTree methodExpr) {
        return maker.App((JCTree.JCExpression)methodExpr);
    }

    /**
     * Builds an AST Tree to declare and initialize a variable, with no modifiers.
     *
     * @param type  the type of the variable
     * @param name  the name of the variable
     * @param owner  the element containing the new symbol
     * @param initializer  the initializer expression
     * @return  a VariableDeclTree declaring the new variable
     */
    public VariableTree buildVariableDecl(TypeMirror type,
                                          String name,
                                          Element owner,
                                          ExpressionTree initializer) {
        DetachedVarSymbol sym =
            new DetachedVarSymbol(0, names.fromString(name),
                                  (Type)type, (Symbol)owner);
        VariableTree tree = maker.VarDef(sym, (JCTree.JCExpression)initializer);
        sym.setDeclaration(tree);
        return tree;
    }

    /**
     * Builds an AST Tree to declare and initialize a variable.  The
     * type of the variable is specified by a Tree.
     *
     * @param type  the type of the variable, as a Tree
     * @param name  the name of the variable
     * @param owner  the element containing the new symbol
     * @param initializer  the initializer expression
     * @return  a VariableDeclTree declaring the new variable
     */
    public VariableTree buildVariableDecl(Tree type,
                                          String name,
                                          Element owner,
                                          ExpressionTree initializer) {
        Type typeMirror = (Type)InternalUtils.typeOf(type);
        DetachedVarSymbol sym =
            new DetachedVarSymbol(0, names.fromString(name),
                                  typeMirror, (Symbol)owner);
        JCTree.JCModifiers mods = maker.Modifiers(0);
        JCTree.JCVariableDecl decl = maker.VarDef(mods, sym.name,
                                                  (JCTree.JCExpression)type,
                                                  (JCTree.JCExpression)initializer);
        decl.setType(typeMirror);
        decl.sym = sym;
        sym.setDeclaration(decl);
        return decl;
    }

    /**
     * Builds an AST Tree to refer to a variable.
     *
     * @param decl  the declaration of the variable
     * @return  an IdentifierTree to refer to the variable
     */
    public IdentifierTree buildVariableUse(VariableTree decl) {
        return (IdentifierTree)maker.Ident((JCTree.JCVariableDecl)decl);
    }

    /**
     * Builds an AST Tree to cast the type of an expression.
     *
     * @param type  the type to cast to
     * @param expr  the expression to be cast
     * @return  a cast of the expression to the type
     */
    public TypeCastTree buildTypeCast(TypeMirror type,
                                      ExpressionTree expr) {
        return maker.TypeCast((Type)type, (JCTree.JCExpression)expr);
    }

    /**
     * Builds an AST Tree to assign an expression to a variable.
     *
     * @param variable  the declaration of the variable to assign to
     * @param expr      the expression to be assigned
     * @return  a statement assigning the expression to the variable
     */
    public StatementTree buildAssignment(VariableTree variable,
                                         ExpressionTree expr) {
        return maker.Assignment(TreeInfo.symbolFor((JCTree)variable),
                                (JCTree.JCExpression)expr);
    }

    /**
     * Builds an AST Tree representing a literal value of primitive
     * or String type.
     */
    public LiteralTree buildLiteral(Object value) {
        return maker.Literal(value);
    }

    /**
     * Builds an AST Tree to compare two operands with less than.
     *
     * @param left  the left operand tree
     * @param right  the right operand tree
     * @return  a Tree representing "left < right"
     */
    public BinaryTree buildLessThan(ExpressionTree left, ExpressionTree right) {
        JCTree.JCBinary binary =
            maker.Binary(JCTree.Tag.LT, (JCTree.JCExpression)left,
                         (JCTree.JCExpression)right);
        binary.setType((Type)modelTypes.getPrimitiveType(TypeKind.BOOLEAN));
        return binary;
    }

    /**
     * Builds an AST Tree to dereference an array.
     *
     * @param array  the array to dereference
     * @param index  the index at which to dereference
     * @return  a Tree representing the dereference
     */
    public ArrayAccessTree buildArrayAccess(ExpressionTree array,
                                            ExpressionTree index) {
        ArrayType arrayType = (ArrayType)InternalUtils.typeOf(array);
        JCTree.JCArrayAccess access =
            maker.Indexed((JCTree.JCExpression)array, (JCTree.JCExpression)index);
        access.setType((Type)arrayType.getComponentType());
        return access;
    }

    /**
     * Builds an AST Tree to postincrement an expression (++).
     *
     * @param expression  the value to be postincremented
     * @return  a Tree representing the postincrement
     */
    public UnaryTree buildPostfixIncrement(ExpressionTree expression) {
        JCTree.JCUnary unary =
            maker.Unary(JCTree.Tag.POSTINC, (JCTree.JCExpression)expression);
        unary.setType((Type)InternalUtils.typeOf(expression));
        return unary;
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
