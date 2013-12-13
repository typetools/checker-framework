package checkers.util.test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import checkers.basetype.*;
import checkers.metaquals.TypeQualifiers;
import checkers.types.*;
import checkers.util.*;

/**
 * A simple checker used for testing the checkers framework.  It treates the
 * {@code \@Odd} annotation as a subtype-style qualifier with no special
 * semantics.
 *
 * <p>
 *
 * This checker should only be used for testing the framework.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@TypeQualifiers( { Odd.class } )
public class TestChecker extends BaseTypeChecker {

    private SimpleSubtypeRelation relation;

    protected AnnotationFactory annoFactory;

    /** Represents the {@code \@Odd} annotation. */
    protected AnnotationMirror ODD;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        annoFactory = new AnnotationFactory(processingEnv);
        ODD = this.annoFactory.fromName(Odd.class.getCanonicalName());
    }
    
    @Override
    public AnnotatedTypeFactory createFactory(ProcessingEnvironment env, CompilationUnitTree root) {
        return new AnnotatedTypeFactory(env, root) {
            @Override
            public void annotateImplicit(Tree tree, AnnotatedTypeMirror type) {
//                if (tree instanceof LiteralTree)
                if (tree.getKind() == Tree.Kind.NULL_LITERAL)
                    type.addAnnotation(ODD);
            }
        };
    }
}
