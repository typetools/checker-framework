package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.NullnessAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;

/** Adds {@link Present} to the type of tree, for calls to ofNullable whose argument is @NonNull. */
public class OptionalAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror PRESENT;

    protected final NullnessAnnotatedTypeFactory nullnessTypeFactory;

    public OptionalAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        PRESENT = AnnotationUtils.fromClass(elements, Present.class);
        nullnessTypeFactory = getTypeFactoryOfSubchecker(NullnessChecker.class);

        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new OptionalTreeAnnotator(this));
    }

    private class OptionalTreeAnnotator extends TreeAnnotator {

        public OptionalTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * For a call to ofNullable, if the argument has type @NonNull, make the return type have
         * type @Present.
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (OptionalUtils.isMethodInvocation(
                    tree, "ofNullable", 1, OptionalAnnotatedTypeFactory.this)) {
                // Determine the Nullness annotation on the argument.
                ExpressionTree arg0 = tree.getArguments().get(0);

                final AnnotatedTypeMirror argNullnessType =
                        nullnessTypeFactory.getAnnotatedType(arg0);
                boolean argIsNonNull =
                        AnnotationUtils.containsSameByClass(
                                argNullnessType.getAnnotations(), NonNull.class);
                if (argIsNonNull) {
                    // Remove current Optional annotation and add @Present.
                    type.replaceAnnotation(PRESENT);
                }
            }
            return super.visitMethodInvocation(tree, type);
        }
    }
}
