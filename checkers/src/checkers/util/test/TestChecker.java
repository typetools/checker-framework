package checkers.util.test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;

/**
 * A simple checker used for testing the Checker Framework.  It treates the
 * {@code @Odd} annotation as a subtype-style qualifier with no special
 * semantics.
 *
 * <p>
 *
 * This checker should only be used for testing the framework.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@TypeQualifiers( { Odd.class } )
public final class TestChecker extends BaseTypeChecker {
    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<TestChecker>(this, tree, false);
    }
}
