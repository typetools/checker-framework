package org.checkerframework.dataflow.expression;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.ExplicitThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.NarrowingConversionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.SuperNode;
import org.checkerframework.dataflow.cfg.node.ThisLiteralNode;
import org.checkerframework.dataflow.cfg.node.ValueLiteralNode;
import org.checkerframework.dataflow.cfg.node.WideningConversionNode;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

// The syntax that the Checker Framework uses for Java expressions also includes "<self>" and
// "#1" for formal parameters.  However, there are no special subclasses (AST nodes) for those
// extensions.
/**
 * This class represents a Java expression and its type. It does not represent all possible Java
 * expressions (for example, it does not represent a ternary conditional expression {@code ?:}; use
 * {@link org.checkerframework.dataflow.expression.Unknown} for unrepresentable expressions).
 *
 * <p>This class's representation is like an AST: subparts are also expressions. For declared names
 * (fields, local variables, and methods), it also contains an Element.
 *
 * <p>Each subclass represents a different type of expression, such as {@link
 * org.checkerframework.dataflow.expression.MethodCall}, {@link
 * org.checkerframework.dataflow.expression.ArrayAccess}, {@link
 * org.checkerframework.dataflow.expression.LocalVariable}, etc.
 *
 * @see <a href="https://checkerframework.org/manual/#java-expressions-as-arguments">the syntax of
 *     Java expressions supported by the Checker Framework</a>
 */
public abstract class JavaExpression {
    /** The type of this expression. */
    protected final TypeMirror type;

    /**
     * Create a JavaExpression.
     *
     * @param type the type of the expression
     */
    protected JavaExpression(TypeMirror type) {
        assert type != null;
        this.type = type;
    }

    public TypeMirror getType() {
        return type;
    }

    public abstract boolean containsOfClass(Class<? extends JavaExpression> clazz);

    public boolean containsUnknown() {
        return containsOfClass(Unknown.class);
    }

    /**
     * Returns true if and only if the value this expression stands for cannot be changed (with
     * respect to ==) by a method call. This is the case for local variables, the self reference,
     * final field accesses whose receiver is {@link #isUnassignableByOtherCode}, and binary
     * operations whose left and right operands are both {@link #isUnmodifiableByOtherCode}.
     *
     * @see #isUnmodifiableByOtherCode
     */
    public abstract boolean isUnassignableByOtherCode();

    /**
     * Returns true if and only if the value this expression stands for cannot be changed by a
     * method call, including changes to any of its fields.
     *
     * <p>Approximately, this returns true if the expression is {@link #isUnassignableByOtherCode}
     * and its type is immutable.
     *
     * @see #isUnassignableByOtherCode
     */
    public abstract boolean isUnmodifiableByOtherCode();

    /**
     * Returns true if and only if the two receivers are syntactically identical.
     *
     * @param other the other object to compare to this one
     * @return true if and only if the two receivers are syntactically identical
     */
    @EqualsMethod
    public boolean syntacticEquals(JavaExpression other) {
        return other == this;
    }

    /**
     * Returns true if and only if this receiver contains a receiver that is syntactically equal to
     * {@code other}.
     *
     * @return true if and only if this receiver contains a receiver that is syntactically equal to
     *     {@code other}
     */
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other);
    }

    /**
     * Returns true if and only if {@code other} appears anywhere in this receiver or an expression
     * appears in this receiver such that {@code other} might alias this expression, and that
     * expression is modifiable.
     *
     * <p>This is always true, except for cases where the Java type information prevents aliasing
     * and none of the subexpressions can alias 'other'.
     */
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        return this.equals(other) || store.canAlias(this, other);
    }

    /**
     * Print this verbosely, for debugging.
     *
     * @return a verbose string representation of this
     */
    public String toStringDebug() {
        return String.format(
                "JavaExpression (%s) %s type=%s", getClass().getSimpleName(), toString(), type);
    }

    ///
    /// Static methods
    ///

    /**
     * Returns the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     * Can contain {@link Unknown} as receiver.
     *
     * @return the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     *     Can contain {@link Unknown} as receiver.
     */
    public static FieldAccess fromNodeFieldAccess(
            AnnotationProvider provider, FieldAccessNode node) {
        Node receiverNode = node.getReceiver();
        JavaExpression receiver;
        if (node.isStatic()) {
            receiver = new ClassName(receiverNode.getType());
        } else {
            receiver = fromNode(provider, receiverNode);
        }
        return new FieldAccess(receiver, node);
    }

    /**
     * Returns the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     * Can contain {@link Unknown} as receiver.
     *
     * @return the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     *     Can contain {@link Unknown} as receiver.
     */
    public static ArrayAccess fromArrayAccess(AnnotationProvider provider, ArrayAccessNode node) {
        JavaExpression array = fromNode(provider, node.getArray());
        JavaExpression index = fromNode(provider, node.getIndex());
        return new ArrayAccess(node.getType(), array, index);
    }

    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation of any {@link Node}. Might contain {@link Unknown}.
     */
    public static JavaExpression fromNode(AnnotationProvider provider, Node node) {
        return fromNode(provider, node, false);
    }

    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation of any {@link Node}. Might contain {@link Unknown}.
     */
    public static JavaExpression fromNode(
            AnnotationProvider provider, Node receiverNode, boolean allowNonDeterministic) {
        JavaExpression result = null;
        if (receiverNode instanceof FieldAccessNode) {
            FieldAccessNode fan = (FieldAccessNode) receiverNode;

            if (fan.getFieldName().equals("this")) {
                // For some reason, "className.this" is considered a field access.
                // We right this wrong here.
                result = new ThisReference(fan.getReceiver().getType());
            } else if (fan.getFieldName().equals("class")) {
                // "className.class" is considered a field access. This makes sense,
                // since .class is similar to a field access which is the equivalent
                // of a call to getClass(). However for the purposes of dataflow
                // analysis, and value stores, this is the equivalent of a ClassNameNode.
                result = new ClassName(fan.getReceiver().getType());
            } else {
                result = fromNodeFieldAccess(provider, fan);
            }
        } else if (receiverNode instanceof ExplicitThisLiteralNode) {
            result = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof ThisLiteralNode) {
            result = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof SuperNode) {
            result = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof LocalVariableNode) {
            LocalVariableNode lv = (LocalVariableNode) receiverNode;
            result = new LocalVariable(lv);
        } else if (receiverNode instanceof ArrayAccessNode) {
            ArrayAccessNode a = (ArrayAccessNode) receiverNode;
            result = fromArrayAccess(provider, a);
        } else if (receiverNode instanceof StringConversionNode) {
            // ignore string conversion
            return fromNode(provider, ((StringConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof WideningConversionNode) {
            // ignore widening
            return fromNode(provider, ((WideningConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof NarrowingConversionNode) {
            // ignore narrowing
            return fromNode(provider, ((NarrowingConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof BinaryOperationNode) {
            BinaryOperationNode bopn = (BinaryOperationNode) receiverNode;
            return new BinaryOperation(
                    bopn,
                    fromNode(provider, bopn.getLeftOperand(), allowNonDeterministic),
                    fromNode(provider, bopn.getRightOperand(), allowNonDeterministic));
        } else if (receiverNode instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiverNode;
            result = new ClassName(cn.getType());
        } else if (receiverNode instanceof ValueLiteralNode) {
            ValueLiteralNode vn = (ValueLiteralNode) receiverNode;
            result = new ValueLiteral(vn.getType(), vn);
        } else if (receiverNode instanceof ArrayCreationNode) {
            ArrayCreationNode an = (ArrayCreationNode) receiverNode;
            List<JavaExpression> dimensions = new ArrayList<>();
            for (Node dimension : an.getDimensions()) {
                dimensions.add(fromNode(provider, dimension, allowNonDeterministic));
            }
            List<JavaExpression> initializers = new ArrayList<>();
            for (Node initializer : an.getInitializers()) {
                initializers.add(fromNode(provider, initializer, allowNonDeterministic));
            }
            result = new ArrayCreation(an.getType(), dimensions, initializers);
        } else if (receiverNode instanceof MethodInvocationNode) {
            MethodInvocationNode mn = (MethodInvocationNode) receiverNode;
            MethodInvocationTree t = mn.getTree();
            if (t == null) {
                throw new BugInCF("Unexpected null tree for node: " + mn);
            }
            assert TreeUtils.isUseOfElement(t) : "@AssumeAssertion(nullness): tree kind";
            ExecutableElement invokedMethod = TreeUtils.elementFromUse(t);

            if (allowNonDeterministic || PurityUtils.isDeterministic(provider, invokedMethod)) {
                List<JavaExpression> parameters = new ArrayList<>();
                for (Node p : mn.getArguments()) {
                    parameters.add(fromNode(provider, p));
                }
                JavaExpression methodReceiver;
                if (ElementUtils.isStatic(invokedMethod)) {
                    methodReceiver = new ClassName(mn.getTarget().getReceiver().getType());
                } else {
                    methodReceiver = fromNode(provider, mn.getTarget().getReceiver());
                }
                result = new MethodCall(mn.getType(), invokedMethod, methodReceiver, parameters);
            }
        }

        if (result == null) {
            result = new Unknown(receiverNode.getType());
        }
        return result;
    }

    /**
     * Returns the internal representation of any {@link ExpressionTree}. Might contain {@link
     * Unknown}.
     *
     * @return the internal representation of any {@link ExpressionTree}. Might contain {@link
     *     Unknown}.
     */
    public static JavaExpression fromTree(
            AnnotationProvider provider, ExpressionTree receiverTree) {
        return fromTree(provider, receiverTree, true);
    }
    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation of any {@link ExpressionTree}. Might contain {@link
     *     Unknown}.
     */
    public static JavaExpression fromTree(
            AnnotationProvider provider, ExpressionTree tree, boolean allowNonDeterministic) {
        JavaExpression result;
        switch (tree.getKind()) {
            case ARRAY_ACCESS:
                ArrayAccessTree a = (ArrayAccessTree) tree;
                JavaExpression arrayAccessExpression = fromTree(provider, a.getExpression());
                JavaExpression index = fromTree(provider, a.getIndex());
                result = new ArrayAccess(TreeUtils.typeOf(a), arrayAccessExpression, index);
                break;
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case DOUBLE_LITERAL:
            case FLOAT_LITERAL:
            case INT_LITERAL:
            case LONG_LITERAL:
            case NULL_LITERAL:
            case STRING_LITERAL:
                LiteralTree vn = (LiteralTree) tree;
                result = new ValueLiteral(TreeUtils.typeOf(tree), vn.getValue());
                break;
            case NEW_ARRAY:
                NewArrayTree newArrayTree = (NewArrayTree) tree;
                List<JavaExpression> dimensions = new ArrayList<>();
                if (newArrayTree.getDimensions() != null) {
                    for (ExpressionTree dimension : newArrayTree.getDimensions()) {
                        dimensions.add(fromTree(provider, dimension, allowNonDeterministic));
                    }
                }
                List<JavaExpression> initializers = new ArrayList<>();
                if (newArrayTree.getInitializers() != null) {
                    for (ExpressionTree initializer : newArrayTree.getInitializers()) {
                        initializers.add(fromTree(provider, initializer, allowNonDeterministic));
                    }
                }

                result = new ArrayCreation(TreeUtils.typeOf(tree), dimensions, initializers);
                break;
            case METHOD_INVOCATION:
                MethodInvocationTree mn = (MethodInvocationTree) tree;
                assert TreeUtils.isUseOfElement(mn) : "@AssumeAssertion(nullness): tree kind";
                ExecutableElement invokedMethod = TreeUtils.elementFromUse(mn);
                if (PurityUtils.isDeterministic(provider, invokedMethod) || allowNonDeterministic) {
                    List<JavaExpression> parameters = new ArrayList<>();
                    for (ExpressionTree p : mn.getArguments()) {
                        parameters.add(fromTree(provider, p));
                    }
                    JavaExpression methodReceiver;
                    if (ElementUtils.isStatic(invokedMethod)) {
                        methodReceiver = new ClassName(TreeUtils.typeOf(mn.getMethodSelect()));
                    } else {
                        methodReceiver = getReceiver(mn, provider);
                    }
                    TypeMirror type = TreeUtils.typeOf(mn);
                    result = new MethodCall(type, invokedMethod, methodReceiver, parameters);
                } else {
                    result = null;
                }
                break;
            case MEMBER_SELECT:
                result = fromMemberSelect(provider, (MemberSelectTree) tree);
                break;
            case IDENTIFIER:
                IdentifierTree identifierTree = (IdentifierTree) tree;
                TypeMirror typeOfId = TreeUtils.typeOf(identifierTree);
                if (identifierTree.getName().contentEquals("this")
                        || identifierTree.getName().contentEquals("super")) {
                    result = new ThisReference(typeOfId);
                    break;
                }
                assert TreeUtils.isUseOfElement(identifierTree)
                        : "@AssumeAssertion(nullness): tree kind";
                Element ele = TreeUtils.elementFromUse(identifierTree);
                if (ElementUtils.isTypeElement(ele)) {
                    result = new ClassName(ele.asType());
                    break;
                }
                switch (ele.getKind()) {
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case EXCEPTION_PARAMETER:
                    case PARAMETER:
                        result = new LocalVariable(ele);
                        break;
                    case FIELD:
                        // Implicit access expression, such as "this" or a class name
                        JavaExpression fieldAccessExpression;
                        @SuppressWarnings(
                                "nullness:dereference.of.nullable") // a field has enclosing class
                        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
                        if (ElementUtils.isStatic(ele)) {
                            fieldAccessExpression = new ClassName(enclosingType);
                        } else {
                            fieldAccessExpression = new ThisReference(enclosingType);
                        }
                        result =
                                new FieldAccess(
                                        fieldAccessExpression, typeOfId, (VariableElement) ele);
                        break;
                    default:
                        result = null;
                }
                break;
            case UNARY_PLUS:
                return fromTree(
                        provider, ((UnaryTree) tree).getExpression(), allowNonDeterministic);
            default:
                result = null;
        }

        if (result == null) {
            result = new Unknown(TreeUtils.typeOf(tree));
        }
        return result;
    }

    private static JavaExpression fromMemberSelect(
            AnnotationProvider provider, MemberSelectTree memberSelectTree) {
        TypeMirror expressionType = TreeUtils.typeOf(memberSelectTree.getExpression());
        if (TreeUtils.isClassLiteral(memberSelectTree)) {
            return new ClassName(expressionType);
        }
        assert TreeUtils.isUseOfElement(memberSelectTree) : "@AssumeAssertion(nullness): tree kind";
        Element ele = TreeUtils.elementFromUse(memberSelectTree);
        if (ElementUtils.isTypeElement(ele)) {
            // o instanceof MyClass.InnerClass
            // o instanceof MyClass.InnerInterface
            TypeMirror selectType = TreeUtils.typeOf(memberSelectTree);
            return new ClassName(selectType);
        }
        switch (ele.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
                return fromTree(provider, memberSelectTree.getExpression());
            case ENUM_CONSTANT:
            case FIELD:
                TypeMirror fieldType = TreeUtils.typeOf(memberSelectTree);
                JavaExpression je = fromTree(provider, memberSelectTree.getExpression());
                return new FieldAccess(je, fieldType, (VariableElement) ele);
            default:
                throw new BugInCF("Unexpected element kind: %s element: %s", ele.getKind(), ele);
        }
    }

    /**
     * Returns the formal parameters of the method in which path is enclosed.
     *
     * @param annotationProvider annotationProvider
     * @param path TreePath that is enclosed by the method
     * @return the formal parameters of the method in which path is enclosed, {@code null} otherwise
     */
    public static @Nullable List<JavaExpression> getParametersOfEnclosingMethod(
            AnnotationProvider annotationProvider, TreePath path) {
        MethodTree methodTree = TreeUtils.enclosingMethod(path);
        if (methodTree == null) {
            return null;
        }
        List<JavaExpression> internalArguments = new ArrayList<>();
        for (VariableTree arg : methodTree.getParameters()) {
            internalArguments.add(fromNode(annotationProvider, new LocalVariableNode(arg)));
        }
        return internalArguments;
    }

    ///
    /// Obtaining the receiver
    ///

    /**
     * Returns the receiver of ele, whether explicit or implicit.
     *
     * @param accessTree method or constructor invocation
     * @param provider an AnnotationProvider
     * @return the receiver of ele, whether explicit or implicit
     */
    public static JavaExpression getReceiver(
            ExpressionTree accessTree, AnnotationProvider provider) {
        // TODO: Handle field accesses too?
        assert accessTree instanceof MethodInvocationTree || accessTree instanceof NewClassTree;
        ExpressionTree receiverTree = TreeUtils.getReceiverTree(accessTree);
        if (receiverTree != null) {
            return fromTree(provider, receiverTree);
        } else {
            Element ele = TreeUtils.elementFromUse(accessTree);
            if (ele == null) {
                throw new BugInCF("TreeUtils.elementFromUse(" + accessTree + ") => null");
            }
            return getImplicitReceiver(ele);
        }
    }

    /**
     * Returns the implicit receiver of ele.
     *
     * <p>Returns either a new ClassName or a new ThisReference depending on whether ele is static
     * or not. The passed element must be a field, method, or class.
     *
     * @param ele field, method, or class
     * @return either a new ClassName or a new ThisReference depending on whether ele is static or
     *     not
     */
    public static JavaExpression getImplicitReceiver(Element ele) {
        TypeElement enclosingClass = ElementUtils.enclosingClass(ele);
        if (enclosingClass == null) {
            throw new BugInCF("getImplicitReceiver's arg has no enclosing class: " + ele);
        }
        TypeMirror enclosingType = enclosingClass.asType();
        if (ElementUtils.isStatic(ele)) {
            return new ClassName(enclosingType);
        } else {
            return new ThisReference(enclosingType);
        }
    }

    /**
     * Returns either a new ClassName or ThisReference JavaExpression object for the enclosingType.
     *
     * <p>The Tree should be an expression or a statement that does not have a receiver or an
     * implicit receiver. For example, a local variable declaration.
     *
     * @param path TreePath to tree
     * @param enclosingType type of the enclosing type
     * @return a new ClassName or ThisReference that is a JavaExpression object for the
     *     enclosingType
     */
    public static JavaExpression getPseudoReceiver(TreePath path, TypeMirror enclosingType) {
        if (TreeUtils.isTreeInStaticScope(path)) {
            return new ClassName(enclosingType);
        } else {
            return new ThisReference(enclosingType);
        }
    }
}
