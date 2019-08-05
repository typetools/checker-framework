package org.checkerframework.checker.nullness;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
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
 *         <li value="1">an empty array initializer, e.g. {@code c.toArray(new String[] {})},
 *         <li value="2">array creation tree of size 0, e.g. {@code c.toArray(new String[0])}, or
 *         <li value="3">array creation tree of the collection size method invocation {@code
 *             c.toArray(new String[c.size()])}
 *       </ol>
 * </ol>
 *
 * Note: The nullness of the returned array doesn't depend on the passed array nullness.
 */
public class CollectionToArrayHeuristics {
    private final ProcessingEnvironment processingEnv;
    private final NullnessAnnotatedTypeFactory atypeFactory;

    private final ExecutableElement collectionToArrayE;
    private final ExecutableElement size;
    private final AnnotatedDeclaredType collectionType;

    public CollectionToArrayHeuristics(
            ProcessingEnvironment env, NullnessAnnotatedTypeFactory factory) {
        this.processingEnv = env;
        this.atypeFactory = factory;

        this.collectionToArrayE =
                TreeUtils.getMethod(java.util.Collection.class.getName(), "toArray", 1, env);
        this.size = TreeUtils.getMethod(java.util.Collection.class.getName(), "size", 0, env);
        this.collectionType =
                factory.fromElement(env.getElementUtils().getTypeElement("java.util.Collection"));
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
            Tree argument = tree.getArguments().get(0);
            boolean argIsArrayCreation =
                    isHandledArrayCreation(argument, receiverName(tree.getMethodSelect()));
            boolean receiverIsNonNull = isNonNullReceiver(tree);
            setComponentNullness(receiverIsNonNull && argIsArrayCreation, method.getReturnType());

            // TODO: We need a mechanism to prevent nullable collections
            // from inserting null elements into a nonnull arrays.
            if (!receiverIsNonNull) {
                setComponentNullness(false, method.getParameterTypes().get(0));
            }
        }
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
