package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A local variable.
 *
 * <p>This class also represents a formal parameter expressed using its name. Subclass {@link
 * FormalParameter} represents a formal parameter expressed using the "#2" notation.
 */
public class LocalVariable extends JavaExpression {
    /** The element for this local variable. */
    protected final Element element;

    /**
     * Creates a new LocalVariable.
     *
     * @param localVar a CFG local variable
     */
    protected LocalVariable(LocalVariableNode localVar) {
        super(localVar.getType());
        this.element = localVar.getElement();
    }

    /**
     * Creates a LocalVariable
     *
     * @param element the element for the local variable
     */
    public LocalVariable(Element element) {
        super(ElementUtils.getType(element));
        this.element = element;
    }

    /**
     * Creates a LocalVariable or a FormalParameter, depending on whether the argument starts with
     * "__param__".
     *
     * @param localVar a CFG node for a variable
     * @return a LocalVariable or FormalParameter for the given CFG variable
     */
    public static LocalVariable create(LocalVariableNode localVar) {
        String name = localVar.getName();
        if (name.startsWith(FormalParameter.PARAMETER_REPLACEMENT)) {
            try {
                return new FormalParameter(
                        Integer.parseInt(
                                name.substring(FormalParameter.PARAMETER_REPLACEMENT_LENGTH)),
                        localVar.getElement());
            } catch (NumberFormatException e) {
                // fallthrough
            }
        }
        return new LocalVariable(localVar);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof LocalVariable)) {
            return false;
        }

        LocalVariable other = (LocalVariable) obj;
        VarSymbol vs1 = (VarSymbol) this.element;
        VarSymbol vs2 = (VarSymbol) other.element;
        // The code below isn't just return vs1.equals(vs2) because an element might be
        // different between subcheckers.  The owner of a lambda parameter is the enclosing
        // method, so a local variable and a lambda parameter might have the same name and the
        // same owner.  pos is used to differentiate this case.
        return vs1.pos == vs2.pos
                && vs1.name.contentEquals(vs2.name)
                && vs1.owner.toString().equals(vs2.owner.toString());
    }

    /**
     * Returns true if this is the same formal parameter as {@code other}.
     *
     * @param je1 the first JavaExpression to compare
     * @param je2 the second JavaExpression to compare
     * @param elements element utilities
     * @return true if this is the same formal parameter as {@code other}
     */
    public static boolean isSameFormalParameter(
            JavaExpression je1, JavaExpression je2, Elements elements) {
        if (je1 instanceof LocalVariable && je2 instanceof LocalVariable) {
            LocalVariable var1 = (LocalVariable) je1;
            LocalVariable var2 = (LocalVariable) je2;
            Element enclosing1 = var1.element.getEnclosingElement();
            Element enclosing2 = var2.element.getEnclosingElement();
            if (enclosing1 instanceof ExecutableElement
                    && enclosing2 instanceof ExecutableElement) {
                ExecutableElement methodElt1 = (ExecutableElement) enclosing1;
                ExecutableElement methodElt2 = (ExecutableElement) enclosing2;
                int index1 = methodElt1.getParameters().indexOf(var1.element);
                int index2 = methodElt2.getParameters().indexOf(var2.element);
                if (index1 != -1 && index1 == index2) {
                    if (elements.overrides(
                                    methodElt1,
                                    methodElt2,
                                    (TypeElement) methodElt1.getEnclosingElement())
                            || elements.overrides(
                                    methodElt2,
                                    methodElt1,
                                    (TypeElement) methodElt2.getEnclosingElement())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the element for this variable.
     *
     * @return the element for this variable
     */
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
    public String toStringDebug() {
        return super.toStringDebug() + " [owner=" + ((VarSymbol) element).owner + "]";
    }

    @Override
    public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
        return getClass() == clazz;
    }

    @Override
    public boolean syntacticEquals(JavaExpression je) {
        if (!(je instanceof LocalVariable)) {
            return false;
        }
        LocalVariable other = (LocalVariable) je;
        return this.equals(other);
    }

    @Override
    public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
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

    @Override
    public LocalVariable atMethodSignature(List<JavaExpression> parameters) {
        int index = parameters.indexOf(this);
        if (index == -1) {
            return this;
        } else {
            return new FormalParameter(index + 1, element);
        }
    }
}
