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

/**
 * Adds {@link Optional} to the type of tree, in the following cases:
 *
 * <ol>
 *   <li value="1">a {@code String} or {@code char} literal that is a valid regular expression
 *   <li value="2">concatenation of two valid regular expression values (either {@code String} or
 *       {@code char}) or two partial regular expression values that make a valid regular expression
 *       when concatenated.
 *   <li value="3">for calls to Pattern.compile changes the group count value of the return type to
 *       be the same as the parameter. For calls to the asOptional methods of the classes in
 *       asOptionalClasses these asOptional methods will return a {@code @Optional String} with the
 *       same group count as the second argument to the call to asOptional.
 *       <!--<li value="4">initialization of a char array that when converted to a String
 * is a valid regular expression.</li>-->
 * </ol>
 *
 * Provides a basic analysis of concatenation of partial regular expressions to determine if a valid
 * regular expression is produced by concatenating non-regular expression Strings. Do do this,
 * {@link PartialOptional} is added to the type of tree in the following cases:
 *
 * <ol>
 *   <li value="1">a String literal that is not a valid regular expression.
 *   <li value="2">concatenation of two partial optional Strings that doesn't result in a optional
 *       String or a partial optional and optional String.
 * </ol>
 *
 * Also, adds {@link PolyOptional} to the type of String/char concatenation of a Optional and a
 * PolyOptional or two PolyOptionals.
 */
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
