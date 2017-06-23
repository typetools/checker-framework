package org.checkerframework.checker.optional;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;

/** Adds {@link Present} to the type of tree, for calls to ofNullable whose argument is @NonNull. */
public class OptionalAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    // ExecutableElement OptionalVisitor.ofNullableMethod

    protected final AnnotationMirror PRESENT;

    public OptionalAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        PRESENT = AnnotationUtils.fromClass(elements, Present.class);

        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new OptionalTreeAnnotator(this);
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

                // How to determine the *NULLNESS* annotation?
                final AnnotatedTypeMirror argType = getAnnotatedType(arg0);
                // TODO
                boolean argIsNonNull = false;

                if (argIsNonNull) {
                    // Remove current @Optional annotation...
                    // ...and add a new one with the correct group count value.
                    type.replaceAnnotation(PRESENT);
                }
            }
            return super.visitMethodInvocation(tree, type);
        }
    }
}
