package org.checkerframework.dataflow.analysis;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
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
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

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
            ExecutableElement invokedMethod = TreeUtils.elementFromUse(mn.getTree());

            // check if this represents a boxing operation of a constant, in which
            // case we treat the method call as deterministic, because there is no way
            // to behave differently in two executions where two constants are being used.
            boolean considerDeterministic = false;
            if (isLongValueOf(mn, invokedMethod)) {
                Node arg = mn.getArgument(0);
                if (arg instanceof ValueLiteralNode) {
                    considerDeterministic = true;
                }
            }

            if (PurityUtils.isDeterministic(provider, invokedMethod)
                    || allowNonDeterministic
                    || considerDeterministic) {
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

    /** Return true iff the invoked method is Long.valueOf(long). */
    private static boolean isLongValueOf(MethodInvocationNode mn, ExecutableElement method) {

        // Less efficient implementation:
        // return method.toString().equals("valueOf(long)")
        //     && mn.getTarget().getReceiver().toString().equals("Long")

        if (mn.getTarget().getReceiver() == null
                || !mn.getTarget().getReceiver().toString().equals("Long")) {
            return false;
        }

        if (!method.getSimpleName().contentEquals("valueOf")) {
            return false;
        }
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() != 1) {
            return false;
        }
        VariableElement param = params.get(0);
        TypeMirror paramType = param.asType();
        return paramType.getKind() == TypeKind.LONG;
    }

    /**
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
                Element ele = TreeUtils.elementFromUse(identifierTree);
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
                    case CLASS:
                    case ENUM:
                    case ANNOTATION_TYPE:
                    case INTERFACE:
                        receiver = new ClassName(ele.asType());
                        break;
                    default:
                        receiver = null;
                }
                break;
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
        TypeMirror enclosingType = ElementUtils.enclosingClass(ele).asType();
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
        Element ele = TreeUtils.elementFromUse(memberSelectTree);
        switch (ele.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
                return internalReprOf(provider, memberSelectTree.getExpression());
            case CLASS: // o instanceof MyClass.InnerClass
            case ENUM:
            case INTERFACE: // o instanceof MyClass.InnerInterface
            case ANNOTATION_TYPE:
                TypeMirror selectType = TreeUtils.typeOf(memberSelectTree);
                return new ClassName(selectType);
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
     *     enclosed
     */
    public static List<Receiver> getParametersOfEnclosingMethod(
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

    /**
     * The poorly-named Receiver class is actually a Java AST. Each subclass represents a different
     * type of expression, such as MethodCall, ArrayAccess, LocalVariable, etc.
     */
    public abstract static class Receiver {
        protected final TypeMirror type;

        public Receiver(TypeMirror type) {
            assert type != null;
            this.type = type;
        }

        public TypeMirror getType() {
            return type;
        }

        public abstract boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz);

        public boolean containsUnknown() {
            return containsOfClass(Unknown.class);
        }

        /**
         * Returns true if and only if the value this expression stands for cannot be changed (with
         * respect to ==) by a method call. This is the case for local variables, the self reference
         * as well as final field accesses for whose receiver {@link #isUnassignableByOtherCode} is
         * true.
         *
         * @see #isUnmodifiableByOtherCode
         */
        public abstract boolean isUnassignableByOtherCode();

        /**
         * Returns true if and only if the value this expression stands for cannot be changed by a
         * method call, including changes to any of its fields.
         *
         * <p>Approximately, this returns true if the expression is {@link
         * #isUnassignableByOtherCode} and its type is immutable.
         *
         * @see #isUnassignableByOtherCode
         */
        public abstract boolean isUnmodifiableByOtherCode();

        /** @return true if and only if the two receiver are syntactically identical */
        public boolean syntacticEquals(Receiver other) {
            return other == this;
        }

        /**
         * @return true if and only if this receiver contains a receiver that is syntactically equal
         *     to {@code other}.
         */
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }

        /**
         * Returns true if and only if {@code other} appears anywhere in this receiver or an
         * expression appears in this receiver such that {@code other} might alias this expression,
         * and that expression is modifiable.
         *
         * <p>This is always true, except for cases where the Java type information prevents
         * aliasing and none of the subexpressions can alias 'other'.
         */
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return this.equals(other) || store.canAlias(this, other);
        }
    }

    public static class FieldAccess extends Receiver {
        protected final Receiver receiver;
        protected final VariableElement field;

        public Receiver getReceiver() {
            return receiver;
        }

        public VariableElement getField() {
            return field;
        }

        public FieldAccess(Receiver receiver, FieldAccessNode node) {
            super(node.getType());
            this.receiver = receiver;
            this.field = node.getElement();
        }

        public FieldAccess(Receiver receiver, TypeMirror type, VariableElement fieldElement) {
            super(type);
            this.receiver = receiver;
            this.field = fieldElement;
        }

        public boolean isFinal() {
            return ElementUtils.isFinal(field);
        }

        public boolean isStatic() {
            return ElementUtils.isStatic(field);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) obj;
            return fa.getField().equals(getField()) && fa.getReceiver().equals(getReceiver());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getField(), getReceiver());
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return super.containsModifiableAliasOf(store, other)
                    || receiver.containsModifiableAliasOf(store, other);
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other) || receiver.containsSyntacticEqualReceiver(other);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) other;
            return super.syntacticEquals(other)
                    || (fa.getField().equals(getField())
                            && fa.getReceiver().syntacticEquals(getReceiver()));
        }

        @Override
        public String toString() {
            if (receiver instanceof ClassName) {
                return receiver.getType() + "." + field;
            } else {
                return receiver + "." + field;
            }
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz) || receiver.containsOfClass(clazz);
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return isFinal() && getReceiver().isUnassignableByOtherCode();
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return isUnassignableByOtherCode()
                    && TypesUtils.isImmutableTypeInJdk(getReceiver().type);
        }
    }

    public static class ThisReference extends Receiver {
        public ThisReference(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ThisReference;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "this";
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return other instanceof ThisReference;
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return true;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return TypesUtils.isImmutableTypeInJdk(type);
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return false; // 'this' is not modifiable
        }
    }

    /**
     * A ClassName represents the occurrence of a class as part of a static field access or method
     * invocation.
     */
    public static class ClassName extends Receiver {
        private final String typeString;

        public ClassName(TypeMirror type) {
            super(type);
            typeString = type.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassName)) {
                return false;
            }
            ClassName other = (ClassName) obj;
            return typeString.equals(other.typeString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeString);
        }

        @Override
        public String toString() {
            return typeString + ".class";
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return this.equals(other);
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return true;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return true;
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return false; // not modifiable
        }
    }

    public static class Unknown extends Receiver {
        public Unknown(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "?";
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return true;
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz);
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return false;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return false;
        }
    }

    public static class LocalVariable extends Receiver {
        protected final Element element;

        public LocalVariable(LocalVariableNode localVar) {
            super(localVar.getType());
            this.element = localVar.getElement();
        }

        public LocalVariable(Element elem) {
            super(ElementUtils.getType(elem));
            this.element = elem;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof LocalVariable)) {
                return false;
            }

            LocalVariable other = (LocalVariable) obj;
            VarSymbol vs = (VarSymbol) element;
            VarSymbol vsother = (VarSymbol) other.element;
            // The code below isn't just return vs.equals(vsother) because an element might be
            // different between subcheckers.  The owner of a lambda parameter is the enclosing
            // method, so a local variable and a lambda parameter might have the same name and the
            // same owner.  pos is used to differentiate this case.
            return vs.pos == vsother.pos
                    && vsother.name.contentEquals(vs.name)
                    && vsother.owner.toString().equals(vs.owner.toString());
        }

        public Element getElement() {
            return element;
        }

        @Override
        public int hashCode() {
            VarSymbol vs = (VarSymbol) element;
            return Objects.hash(
                    vs.name.toString(),
                    TypeAnnotationUtils.unannotatedType(vs.type).toString(),
                    vs.owner.toString());
        }

        @Override
        public String toString() {
            return element.toString();
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof LocalVariable)) {
                return false;
            }
            LocalVariable l = (LocalVariable) other;
            return l.equals(this);
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return true;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return TypesUtils.isImmutableTypeInJdk(((VarSymbol) element).type);
        }
    }

    public static class ValueLiteral extends Receiver {

        protected final Object value;

        public ValueLiteral(TypeMirror type, ValueLiteralNode node) {
            super(type);
            value = node.getValue();
        }

        public ValueLiteral(TypeMirror type, Object value) {
            super(type);
            this.value = value;
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            return getClass().equals(clazz);
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return true;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ValueLiteral)) {
                return false;
            }
            ValueLiteral other = (ValueLiteral) obj;
            // TODO:  Can this string comparison be cleaned up?
            // Cannot use Types.isSameType(type, other.type) because we don't have a Types object.
            return type.toString().equals(other.type.toString())
                    && Objects.equals(value, other.value);
        }

        @Override
        public String toString() {
            if (TypesUtils.isString(type)) {
                return "\"" + value + "\"";
            } else if (type.getKind() == TypeKind.LONG) {
                return value.toString() + "L";
            }
            return value == null ? "null" : value.toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, type.toString());
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return this.equals(other);
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            return false; // not modifiable
        }

        public Object getValue() {
            return value;
        }
    }

    /** A call to a @Deterministic method. */
    public static class MethodCall extends Receiver {

        protected final Receiver receiver;
        protected final List<Receiver> parameters;
        protected final ExecutableElement method;

        public MethodCall(
                TypeMirror type,
                ExecutableElement method,
                Receiver receiver,
                List<Receiver> parameters) {
            super(type);
            this.receiver = receiver;
            this.parameters = parameters;
            this.method = method;
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            if (getClass().equals(clazz)) {
                return true;
            }
            if (receiver.containsOfClass(clazz)) {
                return true;
            }
            for (Receiver p : parameters) {
                if (p.containsOfClass(clazz)) {
                    return true;
                }
            }
            return false;
        }

        /** @return the method call receiver (for inspection only - do not modify) */
        public Receiver getReceiver() {
            return receiver;
        }

        /**
         * @return the method call parameters (for inspection only - do not modify any of the
         *     parameters)
         */
        public List<Receiver> getParameters() {
            return Collections.unmodifiableList(parameters);
        }

        /** @return the ExecutableElement for the method call */
        public ExecutableElement getElement() {
            return method;
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            // There is no need to check that the method is deterministic, because a MethodCall is
            // only created for deterministic methods.
            return receiver.isUnmodifiableByOtherCode()
                    && parameters.stream().allMatch(Receiver::isUnmodifiableByOtherCode);
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return isUnassignableByOtherCode();
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other) || receiver.syntacticEquals(other);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof MethodCall)) {
                return false;
            }
            MethodCall otherMethod = (MethodCall) other;
            if (!receiver.syntacticEquals(otherMethod.receiver)) {
                return false;
            }
            if (parameters.size() != otherMethod.parameters.size()) {
                return false;
            }
            int i = 0;
            for (Receiver p : parameters) {
                if (!p.syntacticEquals(otherMethod.parameters.get(i))) {
                    return false;
                }
                i++;
            }
            return method.equals(otherMethod.method);
        }

        public boolean containsSyntacticEqualParameter(LocalVariable var) {
            for (Receiver p : parameters) {
                if (p.containsSyntacticEqualReceiver(var)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            if (receiver.containsModifiableAliasOf(store, other)) {
                return true;
            }
            for (Receiver p : parameters) {
                if (p.containsModifiableAliasOf(store, other)) {
                    return true;
                }
            }
            return false; // the method call itself is not modifiable
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodCall)) {
                return false;
            }
            if (method.getKind() == ElementKind.CONSTRUCTOR) {
                return this == obj;
            }
            MethodCall other = (MethodCall) obj;
            return parameters.equals(other.parameters)
                    && receiver.equals(other.receiver)
                    && method.equals(other.method);
        }

        @Override
        public int hashCode() {
            if (method.getKind() == ElementKind.CONSTRUCTOR) {
                return super.hashCode();
            }
            return Objects.hash(method, receiver, parameters);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            if (receiver instanceof ClassName) {
                result.append(receiver.getType());
            } else {
                result.append(receiver);
            }
            result.append(".");
            String methodName = method.getSimpleName().toString();
            result.append(methodName);
            result.append("(");
            boolean first = true;
            for (Receiver p : parameters) {
                if (!first) {
                    result.append(", ");
                }
                result.append(p.toString());
                first = false;
            }
            result.append(")");
            return result.toString();
        }
    }

    /** An array access. */
    public static class ArrayAccess extends Receiver {

        protected final Receiver receiver;
        protected final Receiver index;

        public ArrayAccess(TypeMirror type, Receiver receiver, Receiver index) {
            super(type);
            this.receiver = receiver;
            this.index = index;
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            if (getClass().equals(clazz)) {
                return true;
            }
            if (receiver.containsOfClass(clazz)) {
                return true;
            }
            return index.containsOfClass(clazz);
        }

        public Receiver getReceiver() {
            return receiver;
        }

        public Receiver getIndex() {
            return index;
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return false;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return false;
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other)
                    || receiver.syntacticEquals(other)
                    || index.syntacticEquals(other);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof ArrayAccess)) {
                return false;
            }
            ArrayAccess otherArrayAccess = (ArrayAccess) other;
            if (!receiver.syntacticEquals(otherArrayAccess.receiver)) {
                return false;
            }
            return index.syntacticEquals(otherArrayAccess.index);
        }

        @Override
        public boolean containsModifiableAliasOf(Store<?> store, Receiver other) {
            if (receiver.containsModifiableAliasOf(store, other)) {
                return true;
            }
            return index.containsModifiableAliasOf(store, other);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ArrayAccess)) {
                return false;
            }
            ArrayAccess other = (ArrayAccess) obj;
            return receiver.equals(other.receiver) && index.equals(other.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receiver, index);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(receiver.toString());
            result.append("[");
            result.append(index.toString());
            result.append("]");
            return result.toString();
        }
    }

    public static class ArrayCreation extends Receiver {

        protected final List<Receiver> dimensions;
        protected final List<Receiver> initializers;

        public ArrayCreation(
                TypeMirror type, List<Receiver> dimensions, List<Receiver> initializers) {
            super(type);
            this.dimensions = dimensions;
            this.initializers = initializers;
        }

        public List<Receiver> getDimensions() {
            return dimensions;
        }

        public List<Receiver> getInitializers() {
            return initializers;
        }

        @Override
        public boolean containsOfClass(Class<? extends FlowExpressions.Receiver> clazz) {
            for (Receiver n : dimensions) {
                if (n.getClass().equals(clazz)) {
                    return true;
                }
            }
            for (Receiver n : initializers) {
                if (n.getClass().equals(clazz)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isUnassignableByOtherCode() {
            return false;
        }

        @Override
        public boolean isUnmodifiableByOtherCode() {
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimensions, initializers, getType().toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ArrayCreation)) {
                return false;
            }
            ArrayCreation other = (ArrayCreation) obj;
            return this.dimensions.equals(other.getDimensions())
                    && this.initializers.equals(other.getInitializers())
                    // It might be better to use Types.isSameType(getType(), other.getType()), but I
                    // don't have a Types object.
                    && getType().toString().equals(other.getType().toString());
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return this.equals(other);
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("new " + type);
            if (!dimensions.isEmpty()) {
                boolean needComma = false;
                sb.append(" (");
                for (Receiver dim : dimensions) {
                    if (needComma) {
                        sb.append(", ");
                    }
                    sb.append(dim);
                    needComma = true;
                }
                sb.append(")");
            }
            if (!initializers.isEmpty()) {
                boolean needComma = false;
                sb.append(" = {");
                for (Receiver init : initializers) {
                    if (needComma) {
                        sb.append(", ");
                    }
                    sb.append(init);
                    needComma = true;
                }
                sb.append("}");
            }
            return sb.toString();
        }
    }
}
