package checkers.util.test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@TypeQualifiers( { Odd.class } )
public final class FlowTestChecker extends BaseTypeChecker {
    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<FlowTestChecker>(this, tree, true);
    }
}
