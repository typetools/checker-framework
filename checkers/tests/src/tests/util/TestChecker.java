package tests.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import com.sun.source.tree.CompilationUnitTree;

/**
 * A simple checker used for testing the Checker Framework. It treats the
 * {@code @Odd} and {@code @Even} annotations as a subtype-style qualifiers with
 * no special semantics.
 *
 * <p>
 * This checker should only be used for testing the framework.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@TypeQualifiers({ Odd.class, MonoOdd.class, Even.class, Unqualified.class,
        Bottom.class })
public final class TestChecker extends BaseTypeChecker {
    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<TestChecker>(this, tree, false);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        AnnotationUtils annoFactory = AnnotationUtils
                .getInstance(processingEnv);
        return new GraphQualifierHierarchy.GraphFactory(this,
                annoFactory.fromClass(Bottom.class));
    }
}
