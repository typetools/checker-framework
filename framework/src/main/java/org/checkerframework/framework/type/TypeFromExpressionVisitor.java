package org.checkerframework.framework.type;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.WildcardTree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeMerger;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;

/** Converts ExpressionTrees into AnnotatedTypeMirrors. */
class TypeFromExpressionVisitor extends TypeFromTreeVisitor {

    @Override
    public AnnotatedTypeMirror visitAnnotatedType(AnnotatedTypeTree node, AnnotatedTypeFactory f) {
        return f.fromTypeTree(node);
    }

    @Override
    public AnnotatedTypeMirror visitArrayAccess(ArrayAccessTree node, AnnotatedTypeFactory f) {

        Pair<Tree, AnnotatedTypeMirror> preAssCtxt = f.visitorState.getAssignmentContext();
        try {
            // TODO: what other trees shouldn't maintain the context?
            f.visitorState.setAssignmentContext(null);

            AnnotatedTypeMirror type = f.getAnnotatedType(node.getExpression());
            assert type instanceof AnnotatedArrayType;
            return ((AnnotatedArrayType) type).getComponentType();
        } finally {
            f.visitorState.setAssignmentContext(preAssCtxt);
        }
    }

    @Override
    public AnnotatedTypeMirror visitAssignment(AssignmentTree node, AnnotatedTypeFactory f) {

        // Recurse on the type of the variable.
        return visit(node.getVariable(), f);
    }

    @Override
    public AnnotatedTypeMirror visitBinary(BinaryTree node, AnnotatedTypeFactory f) {
        AnnotatedTypeMirror res = f.type(node);
        // TODO: why do we need to clear the type?
        res.clearAnnotations();
        return res;
    }

    @Override
    public AnnotatedTypeMirror visitCompoundAssignment(
            CompoundAssignmentTree node, AnnotatedTypeFactory f) {

        // Recurse on the type of the variable.
        AnnotatedTypeMirror res = visit(node.getVariable(), f);
        // TODO: why do we need to clear the type?
        res.clearAnnotations();
        return res;
    }

    @Override
    public AnnotatedTypeMirror visitConditionalExpression(
            ConditionalExpressionTree node, AnnotatedTypeFactory f) {
        // The Java type of a conditional expression is generally the LUB of the boxed types
        // of the true and false expressions, but with a few exceptions. See JLS 15.25.
        // So, use the type of the ConditionalExpressionTree instead of
        // InternalUtils#leastUpperBound
        TypeMirror alub = TreeUtils.typeOf(node);

        AnnotatedTypeMirror trueType = f.getAnnotatedType(node.getTrueExpression());
        AnnotatedTypeMirror falseType = f.getAnnotatedType(node.getFalseExpression());

        return AnnotatedTypes.leastUpperBound(f, trueType, falseType, alub);
    }

    @Override
    public AnnotatedTypeMirror visitIdentifier(IdentifierTree node, AnnotatedTypeFactory f) {
        if (node.getName().contentEquals("this") || node.getName().contentEquals("super")) {
            AnnotatedDeclaredType res = f.getSelfType(node);
            return res;
        }

        Element elt = TreeUtils.elementFromUse(node);
        AnnotatedTypeMirror selfType = f.getImplicitReceiverType(node);
        if (selfType != null) {
            return AnnotatedTypes.asMemberOf(f.types, f, selfType, elt).asUse();
        }

        return f.getAnnotatedType(elt);
    }

    @Override
    public AnnotatedTypeMirror visitInstanceOf(InstanceOfTree node, AnnotatedTypeFactory f) {
        return f.type(node);
    }

    @Override
    public AnnotatedTypeMirror visitLiteral(LiteralTree node, AnnotatedTypeFactory f) {
        return f.type(node);
    }

    @Override
    public AnnotatedTypeMirror visitMemberSelect(MemberSelectTree node, AnnotatedTypeFactory f) {
        Element elt = TreeUtils.elementFromUse(node);

        if (TreeUtils.isClassLiteral(node)) {
            // the type of a class literal is the type of the "class" element.
            return f.getAnnotatedType(elt);
        }
        switch (elt.getKind()) {
            case METHOD:
            case PACKAGE: // "java.lang" in new java.lang.Short("2")
            case CLASS: // o instanceof MyClass.InnerClass
            case ENUM:
            case INTERFACE: // o instanceof MyClass.InnerInterface
            case ANNOTATION_TYPE:
                return f.fromElement(elt);
            default:
                // Fall-through.
        }

        if (node.getIdentifier().contentEquals("this")) {
            // TODO: why don't we use getSelfType here?
            return f.getEnclosingType(
                    (TypeElement) TreeUtils.elementFromTree(node.getExpression()), node);
        } else {
            // node must be a field access, so get the type of the expression, and then call
            // asMemberOf.
            AnnotatedTypeMirror t = f.getAnnotatedType(node.getExpression());
            return AnnotatedTypes.asMemberOf(f.types, f, t, elt).asUse();
        }
    }

    @Override
    public AnnotatedTypeMirror visitMethodInvocation(
            MethodInvocationTree node, AnnotatedTypeFactory f) {

        AnnotatedExecutableType ex = f.methodFromUse(node).methodType;
        return ex.getReturnType().asUse();
    }

    @Override
    public AnnotatedTypeMirror visitNewArray(NewArrayTree node, AnnotatedTypeFactory f) {

        // Don't use fromTypeTree here, because node.getType() is not an
        // array type!
        AnnotatedArrayType result = (AnnotatedArrayType) f.type(node);

        if (node.getType() == null) { // e.g., byte[] b = {(byte)1, (byte)2};
            return result;
        }

        annotateArrayAsArray(result, node, f);

        return result;
    }

    private AnnotatedTypeMirror descendBy(AnnotatedTypeMirror type, int depth) {
        AnnotatedTypeMirror result = type;
        while (depth > 0) {
            result = ((AnnotatedArrayType) result).getComponentType();
            depth--;
        }
        return result;
    }

    private void annotateArrayAsArray(
            AnnotatedArrayType result, NewArrayTree node, AnnotatedTypeFactory f) {
        // Copy annotations from the type.
        AnnotatedTypeMirror treeElem = f.fromTypeTree(node.getType());
        boolean hasInit = node.getInitializers() != null;
        AnnotatedTypeMirror typeElem = descendBy(result, hasInit ? 1 : node.getDimensions().size());
        while (true) {
            typeElem.addAnnotations(treeElem.getAnnotations());
            if (!(treeElem instanceof AnnotatedArrayType)) {
                break;
            }
            assert typeElem instanceof AnnotatedArrayType;
            treeElem = ((AnnotatedArrayType) treeElem).getComponentType();
            typeElem = ((AnnotatedArrayType) typeElem).getComponentType();
        }
        // Add all dimension annotations.
        int idx = 0;
        AnnotatedTypeMirror level = result;
        while (level.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType array = (AnnotatedArrayType) level;
            List<? extends AnnotationMirror> annos =
                    TreeUtils.annotationsFromArrayCreation(node, idx++);
            array.addAnnotations(annos);
            level = array.getComponentType();
        }

        // Add top-level annotations.
        result.addAnnotations(TreeUtils.annotationsFromArrayCreation(node, -1));
    }

    /**
     * Creates an AnnotatedDeclaredType for the NewClassTree and adds, for each hierarchy, one of:
     *
     * <ul>
     *   <li>an explicit annotation on the new class expression ({@code new @HERE MyClass()}), or
     *   <li>an explicit annotation on the declaration of the class ({@code @HERE class MyClass
     *       {}}), or
     *   <li>an explicit annotation on the declaration of the constructor ({@code @HERE public
     *       MyClass() {}}), or
     *   <li>no annotation for this hierarchy.
     * </ul>
     *
     * @param node NewClassTree
     * @param f the type factory
     * @return AnnotatedDeclaredType of {@code node}
     */
    @Override
    public AnnotatedTypeMirror visitNewClass(NewClassTree node, AnnotatedTypeFactory f) {
        // constructorFromUse return type has implicits
        // so use fromNewClass which does diamond inference and only
        // contains explicit annotations and those inherited from the class declaration
        AnnotatedDeclaredType type = f.fromNewClass(node);

        // Enum constructors lead to trouble.
        // TODO: is there more to check? Can one annotate them?
        if (isNewEnum(type)) {
            return type;
        }

        // Add annotations that are on the constructor declaration.
        // constructorFromUse gives us resolution of polymorphic qualifiers.
        // However, it also applies defaulting, so we might apply too many qualifiers.
        // Therefore, ensure to only add the qualifiers that are explicitly on
        // the constructor, but then take the possibly substituted qualifier.
        AnnotatedExecutableType ex = f.constructorFromUse(node).methodType;
        AnnotatedTypes.copyOnlyExplicitConstructorAnnotations(f, type, ex);

        return type;
    }

    @Override
    public AnnotatedTypeMirror visitMemberReference(
            MemberReferenceTree node, AnnotatedTypeFactory f) {

        AnnotatedDeclaredType type =
                (AnnotatedDeclaredType) f.toAnnotatedType(TreeUtils.typeOf(node), false);
        return type;
    }

    @Override
    public AnnotatedTypeMirror visitLambdaExpression(
            LambdaExpressionTree node, AnnotatedTypeFactory f) {

        AnnotatedDeclaredType type =
                (AnnotatedDeclaredType) f.toAnnotatedType(TreeUtils.typeOf(node), false);
        return type;
    }

    private boolean isNewEnum(AnnotatedDeclaredType type) {
        return type.getUnderlyingType().asElement().getKind() == ElementKind.ENUM;
    }

    @Override
    public AnnotatedTypeMirror visitParenthesized(ParenthesizedTree node, AnnotatedTypeFactory f) {

        // Recurse on the expression inside the parens.
        return visit(node.getExpression(), f);
    }

    @Override
    public AnnotatedTypeMirror visitTypeCast(TypeCastTree node, AnnotatedTypeFactory f) {

        // Use the annotated type of the type in the cast.
        return f.fromTypeTree(node.getType());
    }

    @Override
    public AnnotatedTypeMirror visitUnary(UnaryTree node, AnnotatedTypeFactory f) {
        // TODO: why not visit(node.getExpression(), f)
        return f.type(node);
    }

    @Override
    public AnnotatedTypeMirror visitWildcard(WildcardTree node, AnnotatedTypeFactory f) {

        AnnotatedTypeMirror bound = visit(node.getBound(), f);

        AnnotatedTypeMirror result = f.type(node);
        assert result instanceof AnnotatedWildcardType;

        // the first time getSuperBound/getExtendsBound is called the bound of this wildcard will be
        // appropriately initialized where for the type of node, instead of replacing that bound
        // we merge the annotations onto the initialized bound
        // This ensures that the structure of the wildcard will match that created by
        // BoundsInitializer/createType
        if (node.getKind() == Tree.Kind.SUPER_WILDCARD) {
            AnnotatedTypeMerger.merge(bound, ((AnnotatedWildcardType) result).getSuperBound());

        } else if (node.getKind() == Tree.Kind.EXTENDS_WILDCARD) {
            AnnotatedTypeMerger.merge(bound, ((AnnotatedWildcardType) result).getExtendsBound());
        }
        return result;
    }

    @Override
    public AnnotatedTypeMirror visitPrimitiveType(PrimitiveTypeTree node, AnnotatedTypeFactory f) {
        // for e.g. "int.class"
        return f.fromTypeTree(node);
    }

    @Override
    public AnnotatedTypeMirror visitArrayType(ArrayTypeTree node, AnnotatedTypeFactory f) {
        // for e.g. "int[].class"
        return f.fromTypeTree(node);
    }

    @Override
    public AnnotatedTypeMirror visitParameterizedType(
            ParameterizedTypeTree node, AnnotatedTypeFactory f) {
        return f.fromTypeTree(node);
    }

    @Override
    public AnnotatedTypeMirror visitIntersectionType(
            IntersectionTypeTree node, AnnotatedTypeFactory f) {
        return f.fromTypeTree(node);
    }
}
