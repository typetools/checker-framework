package org.checkerframework.dataflow.expression;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;

/** A call to a @Deterministic method. */
public class MethodCall extends JavaExpression {

    protected final JavaExpression receiver;
    protected final List<JavaExpression> arguments;
    protected final ExecutableElement method;

    public MethodCall(
            TypeMirror type,
            ExecutableElement method,
            JavaExpression receiver,
            List<JavaExpression> arguments) {
        super(type);
        this.receiver = receiver;
        this.arguments = arguments;
        this.method = method;
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        if (getClass() == clazz) {
            return true;
        }
        if (receiver.containsOfClass(clazz)) {
            return true;
        }
        for (JavaExpression p : arguments) {
            if (p.containsOfClass(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the method call receiver (for inspection only - do not modify).
     *
     * @return the method call receiver (for inspection only - do not modify)
     */
    public JavaExpression getReceiver() {
        return receiver;
    }

    /**
     * Returns the method call arguments (for inspection only - do not modify any of the arguments).
     *
     * @return the method call arguments (for inspection only - do not modify any of the arguments)
     */
    public List<JavaExpression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    /**
     * Returns the ExecutableElement for the method call.
     *
     * @return the ExecutableElement for the method call
     */
    public ExecutableElement getElement() {
        return method;
    }

    @Override
    public boolean isUnassignableByOtherCode() {
        // There is no need to check that the method is deterministic, because a MethodCall is
        // only created for deterministic methods.
        return receiver.isUnmodifiableByOtherCode()
                && arguments.stream().allMatch(JavaExpression::isUnmodifiableByOtherCode);
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return isUnassignableByOtherCode();
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other)
                || receiver.containsSyntacticEqualJavaExpression(other)
                || JavaExpression.listContainsSyntacticEqualJavaExpression(arguments, other);
    }

    @Override
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof MethodCall)) {
            return false;
        }
        MethodCall other = (MethodCall) je;
        return this.receiver.syntacticEquals(other.receiver)
                && JavaExpression.syntacticEqualsList(this.arguments, other.arguments)
                && method.equals(other.method);
    }

    public boolean containsSyntacticEqualArgument(LocalVariable var) {
        return JavaExpression.listContainsSyntacticEqualJavaExpression(arguments, var);
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        if (receiver.containsModifiableAliasOf(store, other)) {
            return true;
        }
        for (JavaExpression p : arguments) {
            if (p.containsModifiableAliasOf(store, other)) {
                return true;
            }
        }
        return false; // the method call itself is not modifiable
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MethodCall)) {
            return false;
        }
        if (method.getKind() == ElementKind.CONSTRUCTOR) {
            return false;
        }
        MethodCall other = (MethodCall) obj;
        return arguments.equals(other.arguments)
                && receiver.equals(other.receiver)
                && method.equals(other.method);
    }

    @Override
    public int hashCode() {
        if (method.getKind() == ElementKind.CONSTRUCTOR) {
            return super.hashCode();
        }
        return Objects.hash(method, receiver, arguments);
    }

    @Override
    public String toString() {
        StringBuilder preParen = new StringBuilder();
        if (receiver instanceof ClassName) {
            preParen.append(receiver.getType());
        } else {
            preParen.append(receiver);
        }
        preParen.append(".");
        String methodName = method.getSimpleName().toString();
        preParen.append(methodName);
        preParen.append("(");
        StringJoiner result = new StringJoiner(", ", preParen, ")");
        for (JavaExpression argument : arguments) {
            result.add(argument.toString());
        }
        return result.toString();
    }
}
