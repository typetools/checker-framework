package org.checkerframework.framework.util;

import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.ConditionalPostconditionAnnotation;
import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.qual.EnsuresQualifierIf;
import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.checkerframework.framework.qual.PreconditionAnnotation;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.javacutil.BugInCF;

/**
 * A contract represents an annotation on an expression. It is a precondition, postcondition, or
 * conditional postcondition.
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
    public final String expressionString;

    /** The annotation on the type of expression, according to this contract. */
    public final AnnotationMirror annotation;

    /** The annotation that expressed this contract; used for diagnostic messages. */
    public final AnnotationMirror contractAnnotation;

    // This is redundant with the contract's class and is not used in this file, but the field
    // is used by clients, for its fields.
    /** The kind of contract: precondition, postcondition, or conditional postcondition. */
    public final Kind kind;

    /** Enumerates the kinds of contracts. */
    public enum Kind {
        /** A precondition. */
        PRECONDITION(
                "precondition",
                PreconditionAnnotation.class,
                RequiresQualifier.class,
                RequiresQualifier.List.class,
                "value"),
        /** A postcondition. */
        POSTCONDITION(
                "postcondition",
                PostconditionAnnotation.class,
                EnsuresQualifier.class,
                EnsuresQualifier.List.class,
                "value"),
        /** A conditional postcondition. */
        CONDITIONALPOSTCONDITION(
                "conditional postcondition",
                ConditionalPostconditionAnnotation.class,
                EnsuresQualifierIf.class,
                EnsuresQualifierIf.List.class,
                "expression");

        /** Used for constructing error messages. */
        public final String errorKey;

        /** The meta-annotation identifying annotations of this kind. */
        public final Class<? extends Annotation> metaAnnotation;
        /** The built-in framework qualifier for this contract. */
        public final Class<? extends Annotation> frameworkContractClass;
        /** The built-in framework qualifier for repeated occurrences of this contract. */
        public final Class<? extends Annotation> frameworkContractListClass;
        /**
         * The name of the element that contains the Java expressions on which a contract is
         * enforced.
         */
        public final String expressionElementName;

        /**
         * Create a new Kind.
         *
         * @param errorKey used for constructing error messages
         * @param metaAnnotation the meta-annotation identifying annotations of this kind
         * @param frameworkContractClass the built-in framework qualifier for this contract
         * @param frameworkContractListClass the built-in framework qualifier for repeated
         *     occurrences of this contract
         * @param expressionElementName the name of the element that contains the Java expressions
         *     on which a contract is enforced
         */
        Kind(
                String errorKey,
                Class<? extends Annotation> metaAnnotation,
                Class<? extends Annotation> frameworkContractClass,
                Class<? extends Annotation> frameworkContractListClass,
                String expressionElementName) {
            this.errorKey = errorKey;
            this.metaAnnotation = metaAnnotation;
            this.frameworkContractClass = frameworkContractClass;
            this.frameworkContractListClass = frameworkContractListClass;
            this.expressionElementName = expressionElementName;
        }
    }

    /**
     * Creates a new Contract. This should be called only by the constructors for {@link
     * Precondition}, {@link Postcondition}, and {@link ConditionalPostcondition}.
     *
     * @param kind precondition, postcondition, or conditional postcondition
     * @param expressionString the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expressionString} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     */
    private Contract(
            Kind kind,
            String expressionString,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation) {
        this.expressionString = expressionString;
        this.annotation = annotation;
        this.contractAnnotation = contractAnnotation;
        this.kind = kind;
    }

    /**
     * Creates a new Contract.
     *
     * @param kind precondition, postcondition, or conditional postcondition
     * @param expressionString the Java expression that should have a type qualifier
     * @param annotation the type qualifier that {@code expressionString} should have
     * @param contractAnnotation the pre- or post-condition annotation that the programmer wrote;
     *     used for diagnostic messages
     * @param ensuresQualifierIf the ensuresQualifierIf field, for a conditional postcondition
     * @return a new contract
     */
    public static Contract create(
            Kind kind,
            String expressionString,
            AnnotationMirror annotation,
            AnnotationMirror contractAnnotation,
            Boolean ensuresQualifierIf) {
        if ((ensuresQualifierIf != null) != (kind == Kind.CONDITIONALPOSTCONDITION)) {
            throw new BugInCF(
                    "Mismatch: Contract.create(%s, %s, %s, %s, %s)",
                    kind, expressionString, annotation, contractAnnotation, ensuresQualifierIf);
        }
        switch (kind) {
            case PRECONDITION:
                return new Precondition(expressionString, annotation, contractAnnotation);
            case POSTCONDITION:
                return new Postcondition(expressionString, annotation, contractAnnotation);
            case CONDITIONALPOSTCONDITION:
                return new ConditionalPostcondition(
                        expressionString, annotation, contractAnnotation, ensuresQualifierIf);
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
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }

        Contract otherContract = (Contract) o;

        return kind == otherContract.kind
                && Objects.equals(expressionString, otherContract.expressionString)
                && Objects.equals(annotation, otherContract.annotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, expressionString, annotation);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{expressionString=%s, annotation=%s, contractAnnotation=%s}",
                getClass().getSimpleName(), expressionString, annotation, contractAnnotation);
    }

    /** A precondition contract. */
    public static class Precondition extends Contract {
        /**
         * Create a precondition contract.
         *
         * @param expressionString the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expressionString} should have
         * @param contractAnnotation the precondition annotation that the programmer wrote; used for
         *     diagnostic messages
         */
        public Precondition(
                String expressionString,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(Kind.PRECONDITION, expressionString, annotation, contractAnnotation);
        }
    }

    /** A postcondition contract. */
    public static class Postcondition extends Contract {
        /**
         * Create a postcondition contract.
         *
         * @param expressionString the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expressionString} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         */
        public Postcondition(
                String expressionString,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation) {
            super(Kind.POSTCONDITION, expressionString, annotation, contractAnnotation);
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
         * holds. For example, given
         *
         * <pre>
         * {@code @EnsuresNonNullIf(expression="foo", result=false) boolean method()}
         * </pre>
         *
         * {@code foo} is guaranteed to be {@code @NonNull} after a call to {@code method()} that
         * returns {@code false}.
         */
        public final boolean resultValue;

        /**
         * Create a new conditional postcondition.
         *
         * @param expressionString the Java expression that should have a type qualifier
         * @param annotation the type qualifier that {@code expressionString} should have
         * @param contractAnnotation the postcondition annotation that the programmer wrote; used
         *     for diagnostic messages
         * @param resultValue whether the condition is the method returning true or false
         */
        public ConditionalPostcondition(
                String expressionString,
                AnnotationMirror annotation,
                AnnotationMirror contractAnnotation,
                boolean resultValue) {
            super(Kind.CONDITIONALPOSTCONDITION, expressionString, annotation, contractAnnotation);
            this.resultValue = resultValue;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return super.equals(o) && resultValue == ((ConditionalPostcondition) o).resultValue;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), resultValue);
        }

        @Override
        public String toString() {
            String superToString = super.toString();
            return superToString.substring(0, superToString.length() - 1)
                    + ", annoResult="
                    + resultValue
                    + "}";
        }
    }
}
