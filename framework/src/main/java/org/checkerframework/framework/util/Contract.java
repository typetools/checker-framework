package org.checkerframework.framework.util;

import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/**
 * A contract represents an annotation on an expression, along with the kind: precondition,
 * postcondition, or conditional postcondition.
 *
 * @see Precondition
 * @see Postcondition
 * @see ConditionalPostcondition
 */
public abstract class Contract {

    /**
     * The expression for which the condition must hold, such as {@code "foo"} in
     * {@code @RequiresNonNull("foo")}.
     *
     * <p>An annotation like {@code @RequiresNonNull({"a", "b", "c"})} would be represented by
     * multiple Contracts.
     */
    public final String expression;

    /** The annotation that must be on the type of expression, according to this contract. */
    public final AnnotationMirror annotation;

    /** The annotation that expressed this contract; used for diagnostic messages. */
    public final AnnotationMirror contractAnnotation;

    // This is redundant with the contract's class  and is not used in this file, but the field
    // is used by clients.
    /** The kind of contract: precondition, postcondition, or conditional postcondition. */
    public final Kind kind;

    /**
     * Creates a new Contract.
     *
     * @param kind precondition, postcondition, or conditional postcondition
     * @param expression the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expression} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     */
    public Contract(
            Kind kind,
            String expression,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation) {
        this.expression = expression;
        this.annotation = annotation;
        this.contractAnnotation = contractAnnotation;
        this.kind = kind;
    }

    /**
     * Creates a new Contract.
     *
     * @param expression the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expression} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     * @param kind precondition, postcondition, or conditional postcondition
     * @param ensuresQualifierIf the ensuresQualifierIf field, for a conditional postcondition
     */
    public static Contract create(
            Kind kind,
            String expression,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation,
            Boolean ensuresQualifierIf) {
        if ((ensuresQualifierIf != null) != (kind == Kind.CONDITIONALPOSTCONDITION)) {
            throw new BugInCF("Mismatch: ensuresQualifierIf=%s, kind=%s", ensuresQualifierIf, kind);
        }
        switch (kind) {
            case PRECONDITION:
                return new Precondition(expression, annotation, contractAnnotation);
            case POSTCONDITION:
                return new Postcondition(expression, annotation, contractAnnotation);
            case CONDITIONALPOSTCONDITION:
                return new ConditionalPostcondition(
                        expression, annotation, contractAnnotation, ensuresQualifierIf);
            default:
                throw new BugInCF("Unrecognized kind: " + kind);
        }
    }

    // Note that equality requires exact match of the run-time class and that it ignores the
    // `contractAnnotation` field.
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Contract contract = (Contract) o;

        return kind == contract.kind
                && Objects.equals(expression, contract.expression)
                && Objects.equals(annotation, contract.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, expression, annotation);
    }

    /** A precondition contract. */
    public static class Precondition extends Contract {
        /**
         * Create a precondition contract.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the precondition annotation that the programmer wrote; used for
         *     diagnostic messages
         */
        public Precondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(Kind.PRECONDITION, expression, annotation, contractAnnotation);
        }
    }

    /** Enumerates the kinds of contracts. */
    public enum Kind {
        /** A precondition. */
        PRECONDITION("precondition"),
        /** A postcondition. */
        POSTCONDITION("postcondition"),
        /** A conditional postcondition. */
        CONDITIONALPOSTCONDITION("conditional.postcondition");
        /** Used for constructing error messages. */
        public final String errorKey;

        /**
         * Create a new Kind.
         *
         * @param errorKey used for constructing error messages
         */
        Kind(String errorKey) {
            this.errorKey = errorKey;
        }
    }

    /** A postcondition contract. */
    public static class Postcondition extends Contract {
        /**
         * Create a postcondition contract.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         */
        public Postcondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(Kind.POSTCONDITION, expression, annotation, contractAnnotation);
        }
    }

    /**
     * Represents a conditional postcondition that must be verified by {@code BaseTypeVisitor} or
     * one of its subclasses. Automatically extracted from annotations with meta-annotation
     * {@code @ConditionalPostconditionAnnotation}, such as {@code EnsuresNonNullIf}.
     */
    public static class ConditionalPostcondition extends Contract {

        /**
         * The return value for the annotated method that ensures that the conditional postcondition
         * holds. For example, given<br>
         * {@code @EnsuresNonNullIf(expression="foo", result=false) boolean method()}<br>
         * {@code foo} is guaranteed to be {@code @NonNull} after a call to {@code method()} if that
         * call returns {@code false}.
         */
        public final boolean annoResult;

        /**
         * Create a new conditional postcondition.
         *
         * @param expression the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expression} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         * @param annoResult whether the condition is the method returning true or false
         */
        public ConditionalPostcondition(
                String expression,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation,
                boolean annoResult) {
            super(Kind.CONDITIONALPOSTCONDITION, expression, annotation, contractAnnotation);
            this.annoResult = annoResult;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            ConditionalPostcondition that = (ConditionalPostcondition) o;
            return annoResult == that.annoResult;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), annoResult);
        }
    }
}
