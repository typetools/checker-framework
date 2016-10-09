package org.checkerframework.checker.i18n;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.i18n.qual.Localized;
import org.checkerframework.checker.i18n.qual.UnknownLocalized;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

public class I18nAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public I18nAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<Class<? extends Annotation>>(
                        Arrays.asList(Localized.class, UnknownLocalized.class)));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new I18nTreeAnnotator(this)
        );
    }

    /** Do not propagate types through binary/compound operations.
     */
    private class I18nTreeAnnotator extends TreeAnnotator {
        private final AnnotationMirror LOCALIZED;

        public I18nTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
            LOCALIZED = AnnotationUtils.fromClass(elements, Localized.class);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            type.removeAnnotation(LOCALIZED);
            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            type.removeAnnotation(LOCALIZED);
            return null;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(LOCALIZED)) {
                if (tree.getKind() == Tree.Kind.STRING_LITERAL && tree.getValue().equals("")) {
                    type.addAnnotation(LOCALIZED);
                }
            }
            return super.visitLiteral(tree, type);
        }
    }
}
