package tests.util;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;

/**
 * A simple checker used for testing the Checker Framework.  It treats the
 * {@code @Odd} and {@code @Even} annotations as a subtype-style qualifiers with no special
 * semantics.
 *
 * <p>
 *
 * This checker should only be used for testing the framework.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@TypeQualifiers( { Odd.class, Even.class, Unqualified.class } )
public final class TestChecker extends BaseTypeChecker {
    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<TestChecker>(this, tree, false);
    }
}
