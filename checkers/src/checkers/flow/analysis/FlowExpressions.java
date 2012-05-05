package checkers.flow.analysis;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.cfg.node.ClassNameNode;
import checkers.flow.cfg.node.ExplicitThisNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.ImplicitThisLiteralNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.HashCodeUtils;

/**
 * Collection of classes and helper functions to represent Java expressions
 * about which the dataflow analysis can possibly infer facts. Expressions
 * include:
 * <ul>
 * <li>Field accesses (e.g., <em>o.f</em>)</li>
 * <li>Local variables (e.g., <em>l</em>)</li>
 * <li>This reference (e.g., <em>this</em>)</li>
 * <li>Pure method calls (e.g., <em>o.m()</em>)</li>
 * <li>Unknown other expressions to mark that something else was present.</li>
 * </ul>
 * 
 * @author Stefan Heule
 * 
 */
public class FlowExpressions {

    /**
     * @return The internal representation (as {@link FieldAccess}) of a
     *         {@link FieldAccessNode}. Can contain {@link Unknown} as receiver.
     */
    public static FieldAccess internalReprOfFieldAccess(FieldAccessNode node) {
        Receiver receiver;
        Node receiverNode = node.getReceiver();
        receiver = internalReprOf(receiverNode);
        return new FieldAccess(receiver, node);
    }

    /**
     * @return The internal representation (as {@link Receiver}) of any
     *         {@link Node}. Can contain {@link Unknown} as receiver.
     */
    public static Receiver internalReprOf(Node receiverNode) {
        Receiver receiver;
        if (receiverNode instanceof FieldAccessNode) {
            receiver = internalReprOfFieldAccess((FieldAccessNode) receiverNode);
        } else if (receiverNode instanceof ImplicitThisLiteralNode
                || receiverNode instanceof ExplicitThisNode) {
            receiver = new ThisReference(receiverNode.getType());
        } else if (receiverNode instanceof LocalVariableNode) {
            LocalVariableNode lv = (LocalVariableNode) receiverNode;
            receiver = new LocalVariable(lv);
        } else if (receiverNode instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiverNode;
            receiver = new ClassName(cn.getType(), cn.getElement());
        } else {
            receiver = new Unknown(receiverNode.getType());
        }
        return receiver;
    }

    public static abstract class Receiver {
        protected final TypeMirror type;

        public Receiver(TypeMirror type) {
            this.type = type;
        }

        public TypeMirror getType() {
            return type;
        }

        public abstract boolean containsUnknown();

        /**
         * @return True if and only if the two receiver are syntactically
         *         identical.
         */
        public boolean syntacticEquals(Receiver other) {
            return other == this;
        }

        /**
         * @return True if and only if this receiver contains a receiver that is
         *         syntactically equal to {@code other}.
         */
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }

        /**
         * Returns true if and only if {@code other} appear anywhere in this
         * receiver or an expression appears in this receiver such that
         * {@code other} might alias this expression.
         * 
         * <p>
         * 
         * Informal examples include:
         * 
         * <pre>
         *   "a".containsAliasOf("a") == true
         *   "x.f".containsAliasOf("x.f") == true
         *   "x.f".containsAliasOf("y.g") == false
         *   "x.f".containsAliasOf("a") == true // unless information about "x != a" is available
         *   "?".containsAliasOf("a") == true // ? is Unknown, and a can be anything
         * </pre>
         */
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return this.equals(other) || store.canAlias(this, other);
        }
    }

    public static class FieldAccess extends Receiver {
        protected Receiver receiver;
        protected Element field;

        public Receiver getReceiver() {
            return receiver;
        }

        public Element getField() {
            return field;
        }

        public FieldAccess(Receiver receiver, FieldAccessNode node) {
            super(node.getType());
            this.receiver = receiver;
            this.field = node.getElement();
        }

        public FieldAccess(Receiver receiver, TypeMirror type,
                Element fieldElement) {
            super(type);
            this.receiver = receiver;
            this.field = fieldElement;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) obj;
            return fa.getField().equals(getField())
                    && fa.getReceiver().equals(getReceiver());
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(getField(), getReceiver());
        }

        @Override
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return super.containsAliasOf(store, other)
                    || receiver.containsAliasOf(store, other);
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other)
                    || receiver.containsSyntacticEqualReceiver(other);
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof FieldAccess)) {
                return false;
            }
            FieldAccess fa = (FieldAccess) other;
            return super.syntacticEquals(other)
                    || fa.getField().equals(getField())
                    && fa.getReceiver().syntacticEquals(getReceiver());
        }

        @Override
        public String toString() {
            return receiver + "." + field;
        }

        @Override
        public boolean containsUnknown() {
            return receiver.containsUnknown();
        }
    }

    public static class ThisReference extends Receiver {
        public ThisReference(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof ThisReference;
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(0);
        }

        @Override
        public String toString() {
            return "this";
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return other instanceof ThisReference;
        }
    }

    /**
     * A ClassName represents the occurrence of a class as part of a static
     * field access or method invocation.
     */
    public static class ClassName extends Receiver {
        protected Element element;

        public ClassName(TypeMirror type, Element element) {
            super(type);
            this.element = element;
        }

        public Element getElement() {
            return element;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof ClassName)) {
                return false;
            }
            ClassName other = (ClassName) obj;
            return getElement().equals(other.getElement());
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(getElement());
        }

        @Override
        public String toString() {
            return getElement().getSimpleName().toString();
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            return this.equals(other);
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
        public boolean containsAliasOf(CFAbstractStore<?, ?> store,
                Receiver other) {
            return true;
        }

        @Override
        public boolean containsUnknown() {
            return true;
        }

    }

    public static class LocalVariable extends Receiver {
        protected Element element;

        public LocalVariable(LocalVariableNode localVar) {
            super(localVar.getType());
            this.element = localVar.getElement();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof LocalVariable)) {
                return false;
            }
            LocalVariable other = (LocalVariable) obj;
            return other.element.equals(element);
        }

        public Element getElement() {
            return element;
        }

        @Override
        public int hashCode() {
            return HashCodeUtils.hash(element);
        }

        @Override
        public String toString() {
            return element.toString();
        }

        @Override
        public boolean containsUnknown() {
            return false;
        }

        @Override
        public boolean syntacticEquals(Receiver other) {
            if (!(other instanceof LocalVariable)) {
                return false;
            }
            LocalVariable l = (LocalVariable) other;
            return l.getElement().equals(getElement());
        }

        @Override
        public boolean containsSyntacticEqualReceiver(Receiver other) {
            return syntacticEquals(other);
        }
    }

    // TODO: add pure method calls later
    public static class PureMethodCall extends Receiver {

        public PureMethodCall(TypeMirror type) {
            super(type);
        }

        @Override
        public boolean containsUnknown() {
            return false; // TODO: correct implementation
        }
    }

}
