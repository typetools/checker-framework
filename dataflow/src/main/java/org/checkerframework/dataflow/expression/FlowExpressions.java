package org.checkerframework.dataflow.expression;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
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
import org.checkerframework.checker.nullness.qual.Nullable;
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

/**
 * Collection of classes and helper functions to represent Java expressions about which the
 * org.checkerframework.dataflow analysis can possibly infer facts. Expressions include:
 *
 * <ul>
 *   <li>Field accesses (e.g., <em>o.f</em>)
 *   <li>Local variables (e.g., <em>l</em>)
 *   <li>This reference (e.g., <em>this</em>)
 *   <li>Pure method calls (e.g., <em>o.m()</em>)
 *   <li>Unknown other expressions to mark that something else was present.
 * </ul>
 */
public class FlowExpressions {

    /**
     * Returns the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     * Can contain {@link Unknown} as receiver.
     *
     * @return the internal representation (as {@link FieldAccess}) of a {@link FieldAccessNode}.
     *     Can contain {@link Unknown} as receiver.
     */
    public static FieldAccess internalReprOfFieldAccess(
            AnnotationProvider provider, FieldAccessNode node) {
        Receiver receiver;
        Node receiverNode = node.getReceiver();
        if (node.isStatic()) {
            receiver = new ClassName(receiverNode.getType());
        } else {
            receiver = internalReprOf(provider, receiverNode);
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
    public static ArrayAccess internalReprOfArrayAccess(
            AnnotationProvider provider, ArrayAccessNode node) {
        Receiver receiver = internalReprOf(provider, node.getArray());
        Receiver index = internalReprOf(provider, node.getIndex());
        return new ArrayAccess(node.getType(), receiver, index);
    }

    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation (as {@link Receiver}) of any {@link Node}. Might contain
     *     {@link Unknown}.
     */
    public static Receiver internalReprOf(AnnotationProvider provider, Node receiverNode) {
        return internalReprOf(provider, receiverNode, false);
    }

    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation (as {@link Receiver}) of any {@link Node}. Might contain
     *     {@link Unknown}.
     */
    public static Receiver internalReprOf(
            AnnotationProvider provider, Node receiverNode, boolean allowNonDeterministic) {
        Receiver receiver = null;
        if (receiverNode instanceof FieldAccessNode) {
            FieldAccessNode fan = (FieldAccessNode) receiverNode;

            if (fan.getFieldName().equals("this")) {
                // For some reason, "className.this" is considered a field access.
                // We right this wrong here.
                receiver = new ThisReference(fan.getReceiver().getType());
            } else if (fan.getFieldName().equals("class")) {
                // "className.class" is considered a field access. This makes sense,
                // since .class is similar to a field access which is the equivalent
                // of a call to getClass(). However for the purposes of dataflow
                // analysis, and value stores, this is the equivalent of a ClassNameNode.
                receiver = new ClassName(fan.getReceiver().getType());
            } else {
                receiver = internalReprOfFieldAccess(provider, fan);
            }
        } else if (receiverNode instanceof ExplicitThisLiteralNode) {
            receiver = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof ThisLiteralNode) {
            receiver = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof SuperNode) {
            receiver = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof LocalVariableNode) {
            LocalVariableNode lv = (LocalVariableNode) receiverNode;
            receiver = new LocalVariable(lv);
        } else if (receiverNode instanceof ArrayAccessNode) {
            ArrayAccessNode a = (ArrayAccessNode) receiverNode;
            receiver = internalReprOfArrayAccess(provider, a);
        } else if (receiverNode instanceof StringConversionNode) {
            // ignore string conversion
            return internalReprOf(provider, ((StringConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof WideningConversionNode) {
            // ignore widening
            return internalReprOf(provider, ((WideningConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof NarrowingConversionNode) {
            // ignore narrowing
            return internalReprOf(provider, ((NarrowingConversionNode) receiverNode).getOperand());
        } else if (receiverNode instanceof BinaryOperationNode) {
            BinaryOperationNode bopn = (BinaryOperationNode) receiverNode;
            return new BinaryOperation(
                    bopn,
                    internalReprOf(provider, bopn.getLeftOperand(), allowNonDeterministic),
                    internalReprOf(provider, bopn.getRightOperand(), allowNonDeterministic));
        } else if (receiverNode instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiverNode;
            receiver = new ClassName(cn.getType());
        } else if (receiverNode instanceof ValueLiteralNode) {
            ValueLiteralNode vn = (ValueLiteralNode) receiverNode;
            receiver = new ValueLiteral(vn.getType(), vn);
        } else if (receiverNode instanceof ArrayCreationNode) {
            ArrayCreationNode an = (ArrayCreationNode) receiverNode;
            List<Receiver> dimensions = new ArrayList<>();
            for (Node dimension : an.getDimensions()) {
                dimensions.add(internalReprOf(provider, dimension, allowNonDeterministic));
            }
            List<Receiver> initializers = new ArrayList<>();
            for (Node initializer : an.getInitializers()) {
                initializers.add(internalReprOf(provider, initializer, allowNonDeterministic));
            }
            receiver = new ArrayCreation(an.getType(), dimensions, initializers);
        } else if (receiverNode instanceof MethodInvocationNode) {
            MethodInvocationNode mn = (MethodInvocationNode) receiverNode;
            MethodInvocationTree t = mn.getTree();
            if (t == null) {
                throw new BugInCF("Unexpected null tree for node: " + mn);
            }
            assert TreeUtils.isUseOfElement(t) : "@AssumeAssertion(nullness): tree kind";
            ExecutableElement invokedMethod = TreeUtils.elementFromUse(t);

            if (allowNonDeterministic || PurityUtils.isDeterministic(provider, invokedMethod)) {
                List<Receiver> parameters = new ArrayList<>();
                for (Node p : mn.getArguments()) {
                    parameters.add(internalReprOf(provider, p));
                }
                Receiver methodReceiver;
                if (ElementUtils.isStatic(invokedMethod)) {
                    methodReceiver = new ClassName(mn.getTarget().getReceiver().getType());
                } else {
                    methodReceiver = internalReprOf(provider, mn.getTarget().getReceiver());
                }
                receiver = new MethodCall(mn.getType(), invokedMethod, methodReceiver, parameters);
            }
        }

        if (receiver == null) {
            receiver = new Unknown(receiverNode.getType());
        }
        return receiver;
    }

    /**
     * Returns the internal representation (as {@link Receiver}) of any {@link ExpressionTree}.
     * Might contain {@link Unknown}.
     *
     * @return the internal representation (as {@link Receiver}) of any {@link ExpressionTree}.
     *     Might contain {@link Unknown}.
     */
    public static Receiver internalReprOf(
            AnnotationProvider provider, ExpressionTree receiverTree) {
        return internalReprOf(provider, receiverTree, true);
    }
    /**
     * We ignore operations such as widening and narrowing when computing the internal
     * representation.
     *
     * @return the internal representation (as {@link Receiver}) of any {@link ExpressionTree}.
     *     Might contain {@link Unknown}.
     */
    public static Receiver internalReprOf(
            AnnotationProvider provider,
            ExpressionTree receiverTree,
            boolean allowNonDeterministic) {
        Receiver receiver;
        switch (receiverTree.getKind()) {
            case ARRAY_ACCESS:
                ArrayAccessTree a = (ArrayAccessTree) receiverTree;
                Receiver arrayAccessExpression = internalReprOf(provider, a.getExpression());
                Receiver index = internalReprOf(provider, a.getIndex());
                receiver = new ArrayAccess(TreeUtils.typeOf(a), arrayAccessExpression, index);
                break;
            case BOOLEAN_LITERAL:
            case CHAR_LITERAL:
            case DOUBLE_LITERAL:
            case FLOAT_LITERAL:
            case INT_LITERAL:
            case LONG_LITERAL:
            case NULL_LITERAL:
            case STRING_LITERAL:
                LiteralTree vn = (LiteralTree) receiverTree;
                receiver = new ValueLiteral(TreeUtils.typeOf(receiverTree), vn.getValue());
                break;
            case NEW_ARRAY:
                NewArrayTree newArrayTree = (NewArrayTree) receiverTree;
                List<Receiver> dimensions = new ArrayList<>();
                if (newArrayTree.getDimensions() != null) {
                    for (ExpressionTree dimension : newArrayTree.getDimensions()) {
                        dimensions.add(internalReprOf(provider, dimension, allowNonDeterministic));
                    }
                }
                List<Receiver> initializers = new ArrayList<>();
                if (newArrayTree.getInitializers() != null) {
                    for (ExpressionTree initializer : newArrayTree.getInitializers()) {
                        initializers.add(
                                internalReprOf(provider, initializer, allowNonDeterministic));
                    }
                }

                receiver =
                        new ArrayCreation(TreeUtils.typeOf(receiverTree), dimensions, initializers);
                break;
            case METHOD_INVOCATION:
                MethodInvocationTree mn = (MethodInvocationTree) receiverTree;
                assert TreeUtils.isUseOfElement(mn) : "@AssumeAssertion(nullness): tree kind";
                ExecutableElement invokedMethod = TreeUtils.elementFromUse(mn);
                if (PurityUtils.isDeterministic(provider, invokedMethod) || allowNonDeterministic) {
                    List<Receiver> parameters = new ArrayList<>();
                    for (ExpressionTree p : mn.getArguments()) {
                        parameters.add(internalReprOf(provider, p));
                    }
                    Receiver methodReceiver;
                    if (ElementUtils.isStatic(invokedMethod)) {
                        methodReceiver = new ClassName(TreeUtils.typeOf(mn.getMethodSelect()));
                    } else {
                        ExpressionTree methodReceiverTree = TreeUtils.getReceiverTree(mn);
                        if (methodReceiverTree != null) {
                            methodReceiver = internalReprOf(provider, methodReceiverTree);
                        } else {
                            methodReceiver = internalReprOfImplicitReceiver(invokedMethod);
                        }
                    }
                    TypeMirror type = TreeUtils.typeOf(mn);
                    receiver = new MethodCall(type, invokedMethod, methodReceiver, parameters);
                } else {
                    receiver = null;
                }
                break;
            case MEMBER_SELECT:
                receiver = internalReprOfMemberSelect(provider, (MemberSelectTree) receiverTree);
                break;
            case IDENTIFIER:
                IdentifierTree identifierTree = (IdentifierTree) receiverTree;
                TypeMirror typeOfId = TreeUtils.typeOf(identifierTree);
                if (identifierTree.getName().contentEquals("this")
                        || identifierTree.getName().contentEquals("super")) {
                    receiver = new ThisReference(typeOfId);
                    break;
                }
                assert TreeUtils.isUseOfElement(identifierTree)
                        : "@AssumeAssertion(nullness): tree kind";
                Element ele = TreeUtils.elementFromUse(identifierTree);
                if (ElementUtils.isClassElement(ele)) {
                    receiver = new ClassName(ele.asType());
                    break;
                }
                switch (ele.getKind()) {
                    case LOCAL_VARIABLE:
                    case RESOURCE_VARIABLE:
                    case EXCEPTION_PARAMETER:
                    case PARAMETER:
                        receiver = new LocalVariable(ele);
                        break;
                    case FIELD:
                        // Implicit access expression, such as "this" or a class name
                        Receiver fieldAccessExpression;
                        @SuppressWarnings(
                                "nullness:dereference.of.nullable") // a field has enclosing class
                        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
                        if (ElementUtils.isStatic(ele)) {
                            fieldAccessExpression = new ClassName(enclosingType);
                        } else {
                            fieldAccessExpression = new ThisReference(enclosingType);
                        }
                        receiver =
                                new FieldAccess(
                                        fieldAccessExpression, typeOfId, (VariableElement) ele);
                        break;
                    default:
                        receiver = null;
                }
                break;
            case UNARY_PLUS:
                return internalReprOf(
                        provider,
                        ((UnaryTree) receiverTree).getExpression(),
                        allowNonDeterministic);
            default:
                receiver = null;
        }

        if (receiver == null) {
            receiver = new Unknown(TreeUtils.typeOf(receiverTree));
        }
        return receiver;
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
    public static Receiver internalReprOfImplicitReceiver(Element ele) {
        TypeElement enclosingClass = ElementUtils.enclosingClass(ele);
        if (enclosingClass == null) {
            throw new BugInCF(
                    "internalReprOfImplicitReceiver's arg has no enclosing class: " + ele);
        }
        TypeMirror enclosingType = enclosingClass.asType();
        if (ElementUtils.isStatic(ele)) {
            return new ClassName(enclosingType);
        } else {
            return new ThisReference(enclosingType);
        }
    }

    /**
     * Returns either a new ClassName or ThisReference Receiver object for the enclosingType.
     *
     * <p>The Tree should be an expression or a statement that does not have a receiver or an
     * implicit receiver. For example, a local variable declaration.
     *
     * @param path TreePath to tree
     * @param enclosingType type of the enclosing type
     * @return a new ClassName or ThisReference that is a Receiver object for the enclosingType
     */
    public static Receiver internalReprOfPseudoReceiver(TreePath path, TypeMirror enclosingType) {
        if (TreeUtils.isTreeInStaticScope(path)) {
            return new ClassName(enclosingType);
        } else {
            return new ThisReference(enclosingType);
        }
    }

    private static Receiver internalReprOfMemberSelect(
            AnnotationProvider provider, MemberSelectTree memberSelectTree) {
        TypeMirror expressionType = TreeUtils.typeOf(memberSelectTree.getExpression());
        if (TreeUtils.isClassLiteral(memberSelectTree)) {
            return new ClassName(expressionType);
        }
        assert TreeUtils.isUseOfElement(memberSelectTree) : "@AssumeAssertion(nullness): tree kind";
        Element ele = TreeUtils.elementFromUse(memberSelectTree);
        if (ElementUtils.isClassElement(ele)) {
            // o instanceof MyClass.InnerClass
            // o instanceof MyClass.InnerInterface
            TypeMirror selectType = TreeUtils.typeOf(memberSelectTree);
            return new ClassName(selectType);
        }
        switch (ele.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
                return internalReprOf(provider, memberSelectTree.getExpression());
            case ENUM_CONSTANT:
            case FIELD:
                TypeMirror fieldType = TreeUtils.typeOf(memberSelectTree);
                Receiver r = internalReprOf(provider, memberSelectTree.getExpression());
                return new FieldAccess(r, fieldType, (VariableElement) ele);
            default:
                throw new BugInCF("Unexpected element kind: %s element: %s", ele.getKind(), ele);
        }
    }

    /**
     * Returns Receiver objects for the formal parameters of the method in which path is enclosed.
     *
     * @param annotationProvider annotationProvider
     * @param path TreePath that is enclosed by the method
     * @return list of Receiver objects for the formal parameters of the method in which path is
     *     enclosed, {@code null} otherwise
     */
    public static @Nullable List<Receiver> getParametersOfEnclosingMethod(
            AnnotationProvider annotationProvider, TreePath path) {
        MethodTree methodTree = TreeUtils.enclosingMethod(path);
        if (methodTree == null) {
            return null;
        }
        List<Receiver> internalArguments = new ArrayList<>();
        for (VariableTree arg : methodTree.getParameters()) {
            internalArguments.add(internalReprOf(annotationProvider, new LocalVariableNode(arg)));
        }
        return internalArguments;
    }
}
