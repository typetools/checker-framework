package checkers.types;

import checkers.quals.*;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;

/**
 * Provides a way to obtain annotated class types for the parts of a
 * method declaration (return type, throws clauses, method receiver,
 * parameterized type bounds, etc.) given a method invocation. As for
 * {@link AnnotatedClassType}, annotations are presented uniformly, whether
 * they were included via a "default" annotation or written directly as part of
 * the method invocation.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public class AnnotatedMethodType {

    /** The element related to the annotated method. */
    protected ExecutableElement element;

    /** The annotated return type. */
    protected AnnotatedClassType returnType;

    /** Annotated types for each method parameter. */
    protected List<@NonNull AnnotatedClassType> paramTypes;

    /** The annotated receiver type. */
    protected AnnotatedClassType receiverType;

    /** Annotated types for each throws clause. */
    protected List<@NonNull AnnotatedClassType> throwsTypes;

    /** The processing environment (for utilities and context). */
    protected final ProcessingEnvironment env;
    
    AnnotatedMethodType(ProcessingEnvironment env) {
        this.env = env;
    }
    
    /**
     * @return the annotated return type for the method
     */
    public AnnotatedClassType getAnnotatedReturnType() {
        return this.returnType;
    }

    /**
     * @return the annotated receiver type for the method
     */
    public AnnotatedClassType getAnnotatedReceiverType() {
        return this.receiverType;
    }

    /**
     * @return a list of annotated types for each method (formal) parameter
     */
    public List<AnnotatedClassType> getAnnotatedParameterTypes() {
        return this.paramTypes;
    }

    /**
     * @return a list of annotated types for each thrown exception for the
     *         method
     */
    public List<AnnotatedClassType> getAnnotatedThrowsTypes() {
        return this.throwsTypes;
    }

    /**
     * @param e
     *            the element to associate with this type
     */
    void setElement(ExecutableElement e) {
        this.element = e;
    }

    /**
     * @return the element associated with this type
     */
    public ExecutableElement getElement() {
        return this.element;
    }

    @Override
    public String toString() {
        return "\n\treturn type: " + returnType + "\n" + "\tparam types: "
                + paramTypes + "\n" + "\treceiver type: " + receiverType + "\n"
                + "\tthrows types: " + throwsTypes + "\n";
    }
}
