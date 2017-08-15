package org.checkerframework.common.aliasing;

import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.aliasing.qual.LeakedToResult;
import org.checkerframework.common.aliasing.qual.MaybeAliased;
import org.checkerframework.common.aliasing.qual.MaybeLeaked;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.common.aliasing.qual.Unique;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class AliasingAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    private final AnnotationMirror MAYBE_ALIASED, NON_LEAKED, UNIQUE, MAYBE_LEAKED;

    public AliasingAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        MAYBE_ALIASED = AnnotationUtils.fromClass(elements, MaybeAliased.class);
        NON_LEAKED = AnnotationUtils.fromClass(elements, NonLeaked.class);
        UNIQUE = AnnotationUtils.fromClass(elements, Unique.class);
        MAYBE_LEAKED = AnnotationUtils.fromClass(elements, MaybeLeaked.class);
        if (this.getClass().equals(AliasingAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    // @NonLeaked and @LeakedToResult are type qualifiers because of a checker
    // framework limitation (Issue 383). Once the stub parser gets updated to read
    // non-type-qualifers annotations on stub files, this annotation won't be a
    // type qualifier anymore.
    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiersWithoutPolyAll(MaybeLeaked.class);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        CFTransfer ret = new AliasingTransfer(analysis);
        return ret;
    }

    protected class AliasingTreeAnnotator extends TreeAnnotator {

        public AliasingTreeAnnotator(AliasingAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror p) {
            // Copied hack below from SPARTA:
            // This is a horrible hack around the implementation of constructor
            // results (CF treats annotations on constructor results in stub
            // files as if it were a default and therefore ignores it.)
            // This hack ignores any annotation written in the following location:
            // new @A SomeClass();
            AnnotatedTypeMirror defaulted =
                    atypeFactory.constructorFromUse(node).first.getReturnType();
            Set<AnnotationMirror> defaultedSet = defaulted.getAnnotations();
            p.replaceAnnotations(defaultedSet);
            return null;
        }

        @Override
        public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
            type.replaceAnnotation(UNIQUE);
            return super.visitNewArray(node, type);
        }
    }

    @Override
    protected ListTreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(new AliasingTreeAnnotator(this), super.createTreeAnnotator());
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new AliasingQualifierHierarchy(factory);
    }

    protected class AliasingQualifierHierarchy extends MultiGraphQualifierHierarchy {

        protected AliasingQualifierHierarchy(MultiGraphFactory f) {
            super(f);
        }

        @Override
        protected Set<AnnotationMirror> findBottoms(
                Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newbottoms = AnnotationUtils.createAnnotationSet();
            newbottoms.add(UNIQUE);
            newbottoms.add(MAYBE_LEAKED);
            return newbottoms;
        }

        @Override
        protected Set<AnnotationMirror> findTops(
                Map<AnnotationMirror, Set<AnnotationMirror>> supertypes) {
            Set<AnnotationMirror> newtops = AnnotationUtils.createAnnotationSet();
            newtops.add(MAYBE_ALIASED);
            newtops.add(NON_LEAKED);
            return newtops;
        }

        private boolean isLeakedQualifier(AnnotationMirror anno) {
            return AnnotationUtils.areSameByClass(anno, MaybeLeaked.class)
                    || AnnotationUtils.areSameByClass(anno, NonLeaked.class)
                    || AnnotationUtils.areSameByClass(anno, LeakedToResult.class);
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (isLeakedQualifier(superAnno) && isLeakedQualifier(subAnno)) {
                // @LeakedToResult and @NonLeaked were supposed to be
                // non-type-qualifiers annotations.
                // Currently the stub parser does not support non-type-qualifier
                // annotations on receiver parameters (Issue 383), therefore these
                // annotations are implemented as type qualifiers but the
                // warnings related to the hierarchy are ignored.
                return true;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }
}
