package tests.compound;

import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;

import tests.compound.qual.ACCBottom;
import tests.compound.qual.ACCTop;

import com.sun.source.tree.Tree;

@TypeQualifiers({ ACCTop.class, ACCBottom.class })
public class AnotherCompoundCheckerAnnotatedTypeFactory extends
        BaseAnnotatedTypeFactory {

    public AnotherCompoundCheckerAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(),
                new TreeAnnotator(this) {
                    @Override
                    protected Void defaultAction(Tree node,
                            AnnotatedTypeMirror p) {
                        // Just access the subchecker type factories to make
                        // sure they were created properly
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> aliasingATF = getTypeFactoryOfSubchecker(AliasingChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror aliasing = aliasingATF.getAnnotatedType(node);
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF = getTypeFactoryOfSubchecker(ValueChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror value = valueATF.getAnnotatedType(node);
                        return super.defaultAction(node, p);
                    }

                });

    }

}
