package testlib.compound;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import testlib.compound.qual.ACCBottom;
import testlib.compound.qual.ACCTop;

public class AnotherCompoundCheckerAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public AnotherCompoundCheckerAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(ACCTop.class, ACCBottom.class));
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
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> aliasingATF =
                                getTypeFactoryOfSubchecker(AliasingChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror aliasing = aliasingATF.getAnnotatedType(node);
                        GenericAnnotatedTypeFactory<?, ?, ?, ?> valueATF =
                                getTypeFactoryOfSubchecker(ValueChecker.class);
                        @SuppressWarnings("unused")
                        AnnotatedTypeMirror value = valueATF.getAnnotatedType(node);
                        return super.defaultAction(node, p);
                    }
                });
    }
}
