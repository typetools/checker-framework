package testlib.util;

import com.sun.source.tree.Tree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.subtyping.qual.Bottom;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.*;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationBuilder;

/**
 * A simple checker used for testing the Checker Framework. It treats the {@code @Odd} and
 * {@code @Even} annotations as a subtype-style qualifiers with no special semantics.
 *
 * <p>This checker should only be used for testing the framework.
 */
public final class TestChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new TestVisitor(this);
    }
}

class TestVisitor extends BaseTypeVisitor<TestAnnotatedTypeFactory> {

    public TestVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected TestAnnotatedTypeFactory createTypeFactory() {
        return new TestAnnotatedTypeFactory(checker);
    }

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType type, AnnotatedDeclaredType useType, Tree tree) {
        // TODO: super would result in error, b/c of
        // default on classes.
        return true;
    }
}

class TestAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    protected AnnotationMirror BOTTOM;

    public TestAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker, true);
        Elements elements = processingEnv.getElementUtils();
        BOTTOM = AnnotationBuilder.fromClass(elements, Bottom.class);

        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        defs.addCheckedCodeDefault(BOTTOM, TypeUseLocation.LOWER_BOUND);
        AnnotationMirror unqualified = AnnotationBuilder.fromClass(elements, Unqualified.class);
        defs.addCheckedCodeDefault(unqualified, TypeUseLocation.OTHERWISE);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        Odd.class,
                        MonotonicOdd.class,
                        Even.class,
                        Unqualified.class,
                        Bottom.class));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new GraphQualifierHierarchy(factory, BOTTOM);
    }
}
