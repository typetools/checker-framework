package tests.util;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.Bottom;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

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
@TypeQualifiers( { Odd.class, Even.class, Unqualified.class, Bottom.class } )
public final class TestChecker extends BaseTypeChecker {

    protected AnnotationMirror BOTTOM;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        BOTTOM = annoFactory.fromClass(Bottom.class);
        super.initChecker(env);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<TestChecker>(this, tree, false);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, BOTTOM);
    }
}
