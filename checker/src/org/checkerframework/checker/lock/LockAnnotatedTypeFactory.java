package org.checkerframework.checker.lock;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.checker.lock.qual.LockPossiblyHeld;
import org.checkerframework.dataflow.qual.LockingFree;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.Pair;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;

import com.sun.source.tree.Tree;

/**
 * LockAnnotatedTypeFactory builds types with LockHeld and LockPossiblyHeld annotations.
 * LockHeld identifies that an object is being used as a lock and is being held when a
 * given tree is executed. LockPossiblyHeld is the default type qualifier for this
 * hierarchy and applies to all fields, local variables and parameters - hence it does
 * not convey any information other than that it is not LockHeld.
 *
 * However, there are a number of other annotations used in conjunction with these annotations
 * to enforce proper locking. Consult the Lock Checker documentation at
 * http://types.cs.washington.edu/checker-framework/current/checker-framework-manual.html#lock-checker"
 */
public class LockAnnotatedTypeFactory
    extends GenericAnnotatedTypeFactory<CFValue, LockStore, LockTransfer, LockAnalysis> {

    /** Annotation constants */
    protected final AnnotationMirror LOCKHELD, LOCKPOSSIBLYHELD, SIDEEFFECTFREE;

    // Cache for the lock annotations
    protected final Set<Class<? extends Annotation>> lockAnnos;

    public LockAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
        super(checker, useFlow);

        LOCKHELD = AnnotationUtils.fromClass(elements, LockHeld.class);
        LOCKPOSSIBLYHELD = AnnotationUtils.fromClass(elements, LockPossiblyHeld.class);
        SIDEEFFECTFREE = AnnotationUtils.fromClass(elements, SideEffectFree.class);

        Set<Class<? extends Annotation>> tempLockAnnos = new HashSet<>();
        tempLockAnnos.add(LockHeld.class);
        tempLockAnnos.add(LockPossiblyHeld.class);
        lockAnnos = Collections.unmodifiableSet(tempLockAnnos);

        // This alias is only true for the Lock Checker. All other checkers must
        // ignore the @LockingFree annotation.
        addAliasedDeclAnnotation(LockingFree.class,
                SideEffectFree.class,
                AnnotationUtils.fromClass(elements, SideEffectFree.class));

        postInit();
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new LockQualifierHierarchy(factory);
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new LockTreeAnnotator(this)
        );
    }

    private class LockTreeAnnotator extends TreeAnnotator {
        public LockTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }
    }

    @Override
    protected LockAnalysis createFlowAnalysis(List<Pair<VariableElement, CFValue>> fieldValues) {
        return new LockAnalysis(checker, this, fieldValues);
    }

    @Override
    public LockTransfer createFlowTransferFunction(CFAbstractAnalysis<CFValue, LockStore, LockTransfer> analysis) {
        return new LockTransfer((LockAnalysis) analysis,(LockChecker)this.checker);
    }

    protected AnnotatedTypeMirror getDeclaredAndDefaultedAnnotatedType(Tree tree) {
        shouldCache = false;

        AnnotatedTypeMirror type = getAnnotatedType(tree);

        shouldCache = true;

        return type;
    }

    /**
     * @return The list of annotations of the lock type system.
     */
    public Set<Class<? extends Annotation>> getLockAnnotations() {
        return lockAnnos;
    }

    class LockQualifierHierarchy extends GraphQualifierHierarchy {

        public LockQualifierHierarchy(MultiGraphFactory f) {
            super(f, LOCKHELD);
        }

    }
}
