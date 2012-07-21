package checkers.nonnull;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import checkers.basetype.BaseTypeVisitor;
import checkers.commitment.CommitmentChecker;
import checkers.commitment.quals.Committed;
import checkers.commitment.quals.Free;
import checkers.commitment.quals.Unclassified;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.nullness.quals.LazyNonNull;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.MultiGraphQualifierHierarchy;

import com.sun.source.tree.CompilationUnitTree;

// TODO: make all tests in nullness-other-failing pass
// TODO: Get error messages to work.
// TODO: Suppress "fields.uninitialized" warning if the cause for a field not being
//		 initialized is a type error (the uninitialized error shows up before the
//		 error that actually caused the problem, which is confusing)
// TODO/later: Fix documentation of qualifiers.
// TODO/later: Error messages about LazyNonNull don't mention LazyNonNull, but Nullable
// TODO/later: Add "CommittedOnly" and adapt logic to support either as default, and only one annotation can be present, only present on fields

// TODO/later: Make @Unclassified @Nullable the default for local variables.
//		 Current status: @Nullable @Committed is default for local vars due to flow
//		 not working for commitment annotations.

// DONE: Stefan: Fix Constructor Receiver Type / Return type confusion.
// DONE: (Stefan: this seems to work) Brandon: Assert*After*
// DONE: Stefan: don't allow casts between initialization types.
// DONE: Brandon: make sure that method calls on nullable only issues one warning about null-deref,
//		 and not invalid method invocation.
//		 Overriding checkMethodInvocability() and returning true if "dereference.of.nullable" will
//		 be issued so that it won't issue a method invocation error.
// DONE: Stefan: test our checker with our nullness tests
// DONE: Stefan: LazyNonNull
// DONE: Brandon: SuppressWarnings("nonnull"): it appears to work out of the box,
//			see Checker Manual 20.2.1, added "Suppression" test case to show that it works

@TypeQualifiers({ Nullable.class, NonNull.class, Free.class, Committed.class,
        Unclassified.class })
public class NonNullChecker extends CommitmentChecker {

    /** Annotation constants */
    public AnnotationMirror NONNULL, NULLABLE, LAZYNONNULL;

    @Override
    public void initChecker(ProcessingEnvironment processingEnv) {
        super.initChecker(processingEnv);
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);

        NONNULL = annoFactory.fromClass(NonNull.class);
        NULLABLE = annoFactory.fromClass(Nullable.class);
        LAZYNONNULL = annoFactory.fromClass(LazyNonNull.class);
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor(CompilationUnitTree root) {
        return new NonNullVisitor(this, root);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree root) {
        return new NonNullAnnotatedTypeFactory(this, root);
    }

    /**
     * @return The list of annotations of the non-null type system.
     */
    public Set<AnnotationMirror> getNonNullAnnotations() {
        Set<AnnotationMirror> result = new HashSet<>();
        result.add(NONNULL);
        result.add(NULLABLE);
        return result;
    }

    @Override
    public Set<AnnotationMirror> getInvalidConstructorReturnTypeAnnotations() {
        Set<AnnotationMirror> l = new HashSet<>(
                super.getInvalidConstructorReturnTypeAnnotations());
        l.addAll(getNonNullAnnotations());
        return l;
    }

    @Override
    public AnnotationMirror getFieldInvariantAnnotation() {
        return NONNULL;
    }
}
