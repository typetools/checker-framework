package org.checkerframework.dataflow.expression;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.plumelib.util.UtilPlume;

/** A call to a @Deterministic method. */
public class MethodCall extends JavaExpression {

    protected final JavaExpression receiver;
    protected final List<JavaExpression> parameters;
    protected final ExecutableElement method;

    public MethodCall(
            TypeMirror type,
            ExecutableElement method,
            JavaExpression receiver,
            List<JavaExpression> parameters) {
        super(type);
        this.receiver = receiver;
        this.parameters = parameters;
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
        for (JavaExpression p : parameters) {
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
     * Returns the method call parameters (for inspection only - do not modify any of the
     * parameters).
     *
     * @return the method call parameters (for inspection only - do not modify any of the
     *     parameters)
     */
    public List<JavaExpression> getParameters() {
        return Collections.unmodifiableList(parameters);
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
                && parameters.stream().allMatch(JavaExpression::isUnmodifiableByOtherCode);
    }

    @Override
    public boolean isUnmodifiableByOtherCode() {
        return isUnassignableByOtherCode();
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
        return syntacticEquals(other) || receiver.syntacticEquals(other);
    }

    @Override
    public boolean syntacticEquals(JavaExpression other) {
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
        for (JavaExpression p : parameters) {
            if (!p.syntacticEquals(otherMethod.parameters.get(i))) {
                return false;
            }
            i++;
        }
        return method.equals(otherMethod.method);
    }

    public boolean containsSyntacticEqualParameter(LocalVariable var) {
        for (JavaExpression p : parameters) {
            if (p.containsSyntacticEqualJavaExpression(var)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
        if (receiver.containsModifiableAliasOf(store, other)) {
            return true;
        }
        for (JavaExpression p : parameters) {
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
        result.append(UtilPlume.join(", ", parameters));
        result.append(")");
        return result.toString();
    }
}
