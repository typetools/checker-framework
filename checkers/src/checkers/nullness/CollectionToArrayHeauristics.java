package checkers.nullness;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.TreeUtils;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * Handles calls to {@link java.util.Collection#toArray()} and determines
 * the appropriate nullness type of the returned value.
 */
public class CollectionToArrayHeauristics {
    private final ProcessingEnvironment env;
    private final NullnessAnnotatedTypeFactory factory;
    private final AnnotatedTypes atypes;

    private final ExecutableElement collectionToArrayObject;
    private final ExecutableElement collectionToArrayE;
    private final AnnotatedDeclaredType collectionType;

    public CollectionToArrayHeauristics(ProcessingEnvironment env,
            NullnessAnnotatedTypeFactory factory) {
        this.env = env;
        this.factory = factory;
        this.atypes = new AnnotatedTypes(env, factory);

        this.collectionToArrayObject = getMethod("java.util.Collection", "toArray", 0);
        this.collectionToArrayE = getMethod("java.util.Collection", "toArray", 1);
        this.collectionType = factory.fromElement(env.getElementUtils().getTypeElement("java.util.Collection"));
    }

    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (isMethod(tree, collectionToArrayObject)
                || isMethod(tree, collectionToArrayE)) {
            boolean receiver = isNonNullReceiver(tree);
            boolean argument = isNonNullArgument(tree);

            setComponentNullness(receiver && argument, method.getReturnType());

            // TODO: we need a mechanism to prevent nullable collections
            // from inserting null elements into a nonnull arrays
            if (!receiver && argument && !tree.getArguments().isEmpty())
                setComponentNullness(receiver, method.getParameterTypes().get(0));
        }
    }

    private void setComponentNullness(boolean isNonNull, AnnotatedTypeMirror type) {
        assert type.getKind() == TypeKind.ARRAY;
        AnnotatedTypeMirror compType = ((AnnotatedArrayType)type).getComponentType();
        compType.clearAnnotations();
        compType.addAnnotation(isNonNull ? factory.NONNULL : factory.NULLABLE);
    }

    private boolean isNonNullReceiver(MethodInvocationTree tree) {
        // check receiver
        AnnotatedTypeMirror receiver = factory.getReceiver(tree);
        AnnotatedDeclaredType collection = (AnnotatedDeclaredType)atypes.asSuper(receiver, collectionType);
        assert collection != null;

        if (collection.getTypeArguments().isEmpty()
            || !collection.getTypeArguments().get(0).hasAnnotation(factory.NONNULL))
            return false;
        return true;
    }

    private boolean isNonNullArgument(MethodInvocationTree tree) {
        // check based on argument
        if (isMethod(tree, collectionToArrayE)) {
            assert !tree.getArguments().isEmpty() : tree;
            AnnotatedTypeMirror type = factory.getAnnotatedType(tree.getArguments().get(0));
            assert type.getKind() == TypeKind.ARRAY;
            AnnotatedArrayType arType = (AnnotatedArrayType)type;
            return arType.getComponentType().hasAnnotation(factory.NONNULL);
        }

        return true;
    }

    // TODO: duplicated code from MapGetHeauristics
    private boolean isMethod(Tree tree, ExecutableElement method) {
        if (!(tree instanceof MethodInvocationTree))
            return false;
        MethodInvocationTree methInvok = (MethodInvocationTree)tree;
        ExecutableElement invoked = TreeUtils.elementFromUse(methInvok);
        return isMethod(invoked, method);
    }

    private boolean isMethod(ExecutableElement questioned, ExecutableElement method) {
        return (questioned.equals(method)
                || env.getElementUtils().overrides(questioned, method,
                        (TypeElement)questioned.getEnclosingElement()));
    }

    private ExecutableElement getMethod(String typeName, String methodName, int params) {
        TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
        for (ExecutableElement exec : ElementFilter.methodsIn(mapElt.getEnclosedElements())) {
            if (exec.getSimpleName().contentEquals(methodName)
                    && exec.getParameters().size() == params)
                return exec;
        }
        throw new RuntimeException("Shouldn't be here!");
    }

}
