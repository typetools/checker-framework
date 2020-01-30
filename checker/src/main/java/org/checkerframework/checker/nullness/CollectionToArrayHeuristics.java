package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Determines the nullness type of calls to {@link java.util.Collection#toArray()}.
 *
 * <p>The semantics of {@link Collection#toArray(Object[]) Collection.toArray(T[])} cannot be
 * captured by the nullness type system syntax. The nullness type of the returned array depends on
 * the size of the passed parameter. In particular, the returned array component is of type
 * {@code @NonNull} if the following conditions hold:
 *
 * <ol>
 *   <li value="1">The receiver collection type argument is {@code NonNull}, and
 *   <li value="2">The passed array size is less than the collection size. Here are heuristics to
 *       handle the most common cases:
 *       <ol>
 *         <li value="1">the argument has length 0:
 *             <ol>
 *               <li value="1">an empty array initializer, e.g. {@code c.toArray(new String[] {})},
 *                   or
 *               <li value="2">array creation tree of size 0, e.g. {@code c.toArray(new String[0])}.
 *             </ol>
 *         <li value="2">array creation tree with a collection {@code size()} method invocation as
 *             argument {@code c.toArray(new String[c.size()])}
 *       </ol>
 * </ol>
 *
 * <p>Additionally, when the lint option {@link NullnessChecker#LINT_TRUSTARRAYLENZERO} is provided,
 * a call to {@link Collection#toArray(Object[]) Collection.toArray(T[])} will be estimated to
 * return an array with non-null components if the argument is a field access where the field
 * declaration has a {@code @ArrayLen(0)} annotation. This trusts the {@code @ArrayLen(0)}
 * annotation, but does not verify it. Run the Constant Value Checker to verify that annotation.
 *
 * <p>Note: The nullness of the returned array doesn't depend on the passed array nullness. This is
 * a fact about {@link Collection#toArray(Object[]) Collection.toArray(T[])}, not a limitation of
 * this heuristic.
 *
 * @checker_framework.manual #nullness-collection-toarray Nullness and conversions from collections
 *     to arrays
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
// Note: The {@code nullness-collection-toarray} section in the manual should be kept consistent
// with this Javadoc.
public class CollectionToArrayHeuristics {
    /** The processing environment. */
    private final ProcessingEnvironment processingEnv;
    /** The type factory. */
    private final NullnessAnnotatedTypeFactory atypeFactory;

    /** The element for {@link Collection#toArray(Object[])}. */
    private final ExecutableElement collectionToArrayE;
    /** The element for {@link Collection#size()}. */
    private final ExecutableElement size;
    /** The type for {@link Collection}. */
    private final AnnotatedDeclaredType collectionType;
    /** Whether to trust {@code @ArrayLen(0)} annotations. */
    private final boolean trustArrayLenZero;

    /**
     * Create the heuristics for the given nullness checker and factory.
     *
     * @param checker the checker instance
     * @param factory the factory instance
     */
    public CollectionToArrayHeuristics(
            NullnessChecker checker, NullnessAnnotatedTypeFactory factory) {
        this.processingEnv = checker.getProcessingEnvironment();
        this.atypeFactory = factory;

        this.collectionToArrayE =
                TreeUtils.getMethod(
                        java.util.Collection.class.getName(), "toArray", processingEnv, "T[]");
        this.size =
                TreeUtils.getMethod(java.util.Collection.class.getName(), "size", 0, processingEnv);
        this.collectionType =
                factory.fromElement(
                        processingEnv.getElementUtils().getTypeElement("java.util.Collection"));

        this.trustArrayLenZero =
                checker.getLintOption(
                        NullnessChecker.LINT_TRUSTARRAYLENZERO,
                        NullnessChecker.LINT_DEFAULT_TRUSTARRAYLENZERO);
    }

    /**
     * If the method invocation is a call to {@code toArray}, then it manipulates the returned type
     * of {@code method} arg to contain the appropriate nullness. Otherwise, it does nothing.
     *
     * @param tree method invocation tree
     * @param method invoked method type
     */
    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (TreeUtils.isMethodInvocation(tree, collectionToArrayE, processingEnv)) {
            assert !tree.getArguments().isEmpty() : tree;
            ExpressionTree argument = tree.getArguments().get(0);
            boolean receiverIsNonNull = isNonNullReceiver(tree);
            boolean argIsHandled =
                    isHandledArrayCreation(argument, receiverName(tree.getMethodSelect()));
            argIsHandled =
                    argIsHandled || (trustArrayLenZero && isArrayLenZeroFieldAccess(argument));
            setComponentNullness(receiverIsNonNull && argIsHandled, method.getReturnType());

            // TODO: We need a mechanism to prevent nullable collections
            // from inserting null elements into a nonnull arrays.
            if (!receiverIsNonNull) {
                setComponentNullness(false, method.getParameterTypes().get(0));
            }
        }
    }

    /**
     * Determine whether the argument is a field access expression of which the declaration has a
     * {@code ArrayLen(0)} annotation.
     *
     * @param argument the expression tree
     * @return true if the expression is a field access expression, where the field has declared
     *     type {@code ArrayLen(0)}
     */
    private boolean isArrayLenZeroFieldAccess(ExpressionTree argument) {
        Element el = TreeUtils.elementFromUse(argument);
        if (el != null && el.getKind().isField()) {
            TypeMirror t = ElementUtils.getType(el);
            if (t.getKind() == TypeKind.ARRAY) {
                List<? extends AnnotationMirror> ams = t.getAnnotationMirrors();
                for (AnnotationMirror am : ams) {
                    if (AnnotationUtils.areSameByClass(am, ArrayLen.class)) {
                        List<Integer> lens =
                                AnnotationUtils.getElementValueArray(
                                        am, "value", Integer.class, false);
                        if (lens.size() == 1 && lens.get(0) == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sets the nullness of the component of the array type.
     *
     * @param isNonNull indicates which annotation ({@code NonNull} or {@code Nullable}) should be
     *     inserted
     * @param type the array type
     */
    private void setComponentNullness(boolean isNonNull, AnnotatedTypeMirror type) {
        assert type.getKind() == TypeKind.ARRAY;
        AnnotatedTypeMirror compType = ((AnnotatedArrayType) type).getComponentType();
        compType.replaceAnnotation(isNonNull ? atypeFactory.NONNULL : atypeFactory.NULLABLE);
    }

    /**
     * Returns true if {@code argument} is one of the array creation trees that the heuristic
     * handles.
     *
     * @param argument the tree passed to {@link Collection#toArray(Object[])
     *     Collection.toArray(T[])}
     * @param receiver the expression for the receiver collection
     * @return true if the argument is handled and assume to return nonnull elements
     */
    private boolean isHandledArrayCreation(Tree argument, String receiver) {
        if (argument.getKind() != Tree.Kind.NEW_ARRAY) {
            return false;
        }
        NewArrayTree newArr = (NewArrayTree) argument;

        // case 1: empty array initializer
        if (newArr.getInitializers() != null) {
            return newArr.getInitializers().isEmpty();
        }

        assert !newArr.getDimensions().isEmpty();
        Tree dimension = newArr.getDimensions().get(newArr.getDimensions().size() - 1);

        // case 2: 0-length array creation
        if (dimension.toString().equals("0")) {
            return true;
        }

        // case 3: size()-length array creation
        if (TreeUtils.isMethodInvocation(dimension, size, processingEnv)) {
            MethodInvocationTree invok = (MethodInvocationTree) dimension;
            String invokReceiver = receiverName(invok.getMethodSelect());
            return invokReceiver.equals(receiver);
        }

        return false;
    }

    /**
     * Returns {@code true} if the method invocation tree receiver is collection that contains
     * non-null elements (i.e. its type argument is a {@code NonNull}.
     */
    private boolean isNonNullReceiver(MethodInvocationTree tree) {
        // check receiver
        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);
        AnnotatedDeclaredType collection =
                AnnotatedTypes.asSuper(atypeFactory, receiver, collectionType);

        if (collection.getTypeArguments().isEmpty() // raw type
                || !collection
                        .getTypeArguments()
                        .get(0)
                        .hasEffectiveAnnotation(atypeFactory.NONNULL)) {
            return false;
        }
        return true;
    }

    /**
     * The name of the receiver object of the tree.
     *
     * @param tree either an identifier tree or a member select tree
     */
    // This method is quite sloppy, but works most of the time
    private String receiverName(Tree tree) {
        if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
            return ((MemberSelectTree) tree).getExpression().toString();
        } else {
            return "this";
        }
    }
}
