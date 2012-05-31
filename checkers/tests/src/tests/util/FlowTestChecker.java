package tests.util;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.CompilationUnitTree;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;
import checkers.util.GraphQualifierHierarchy;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@TypeQualifiers( { Value.class, Odd.class, Unqualified.class } )
public final class FlowTestChecker extends BaseTypeChecker {

    protected AnnotationMirror VALUE;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        super.initChecker(env);

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        VALUE = annoFactory.fromClass(Value.class);
    }

    @Override
    public AnnotatedTypeFactory createFactory(CompilationUnitTree tree) {
        return new BasicAnnotatedTypeFactory<FlowTestChecker>(this, tree, true);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new FlowQualifierHierarchy((GraphQualifierHierarchy) super.createQualifierHierarchy());
    }

    private final class FlowQualifierHierarchy extends GraphQualifierHierarchy {

        public FlowQualifierHierarchy(GraphQualifierHierarchy hierarchy) {
            super(hierarchy);
        }

        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameIgnoringValues(lhs, VALUE) &&
                    AnnotationUtils.areSameIgnoringValues(rhs, VALUE)) {
                return AnnotationUtils.areSame(lhs, rhs);
            }
            if (AnnotationUtils.areSameIgnoringValues(lhs, VALUE)) {
                lhs = VALUE;
            }
            if (AnnotationUtils.areSameIgnoringValues(rhs, VALUE)) {
                rhs = VALUE;
            }
            return super.isSubtype(rhs, lhs);
        }
    }
}
