package org.checkerframework.framework.testchecker.compound;

import com.sun.source.tree.Tree;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.testchecker.compound.qual.CCBottom;
import org.checkerframework.framework.testchecker.compound.qual.CCTop;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CompoundCheckerAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public CompoundCheckerAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(Arrays.asList(CCTop.class, CCBottom.class));
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new TreeAnnotator(this) {
                    @Override
                    protected Void defaultAction(Tree node, AnnotatedTypeMirror p) {
                        // Just access the subchecker type factories to make
                        // sure they were created properly
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> accATF =
                                getTypeFactoryOfSubchecker(AnotherCompoundChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror another = accATF.getAnnotatedType(node);
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> aliasingATF =
                                getTypeFactoryOfSubchecker(AliasingChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror aliasing = aliasingATF.getAnnotatedType(node);
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF =
                                getTypeFactoryOfSubchecker(ValueChecker.class);
                        assert valueATF == null
                                : "Should not be able to access the ValueChecker annotations.";
                        return super.defaultAction(node, p);
                    }
                });
    }
}
