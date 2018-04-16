package org.checkerframework.framework.util.typeinference8.util;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.InferenceTypeFactory;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Theta;
import org.checkerframework.framework.util.typeinference8.types.typemirror.ProperTypeMirror;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * An object to pass around for use during invocation type inference. One context is created per
 * top-level invocation expression.
 */
public class Java8InferenceContext {

    /** Path to the top level expression whose type arguments are inferred. */
    public final TreePath pathToExpression;

    /** javax.annotation.processing.ProcessingEnvironment */
    public final ProcessingEnvironment env;

    /** ProperType for java.lang.Object. */
    public final ProperType object;

    /** Invocation type inference object. */
    public final InvocationTypeInference inference;

    /** com.sun.tools.javac.code.Types */
    public final Types types;

    /** javax.lang.model.util.Types */
    public final javax.lang.model.util.Types modelTypes;

    /**
     * The type of class that encloses the top level expression whose type arguments are inferred.
     */
    public final DeclaredType enclosingType;

    /**
     * Store previously created type variable to inference variable maps as a map from invocation
     * expression to Theta.
     */
    public final Map<ExpressionTree, Theta> maps;

    /** Number of non-capture variables. */
    private int variableCount = 1;

    /** Number of capture variables. */
    private int captureVariableCount = 1;

    /** TypeMirror for java.lang.RuntimeException. */
    public final TypeMirror runtimeEx;

    public final InferenceTypeFactory inferenceTypeFactory;

    public Java8InferenceContext(
            ProcessingEnvironment env,
            AnnotatedTypeFactory factory,
            TreePath pathToExpression,
            InvocationTypeInference inference) {
        this.pathToExpression = pathToExpression;
        this.env = env;
        this.inference = inference;
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) env;
        this.types = Types.instance(javacEnv.getContext());
        this.modelTypes = factory.getContext().getChecker().getTypeUtils();
        TypeMirror objecTypeMirror =
                TypesUtils.typeFromClass(
                        Object.class,
                        factory.getContext().getTypeUtils(),
                        factory.getElementUtils());
        this.object = new ProperTypeMirror(objecTypeMirror, this);
        ClassTree clazz = TreeUtils.enclosingClass(pathToExpression);
        this.enclosingType = (DeclaredType) TreeUtils.typeOf(clazz);
        this.maps = new HashMap<>();
        this.runtimeEx =
                TypesUtils.typeFromClass(
                        RuntimeException.class, env.getTypeUtils(), env.getElementUtils());
        this.inferenceTypeFactory = new InferenceTypeFactory(this);
    }

    /** @return the next number to use as the id for a non-capture variable */
    public int getNextVariableId() {
        return variableCount++;
    }

    /** @return the next number to use as the id for a capture variable */
    public int getNextCaptureVariableId() {
        return captureVariableCount++;
    }
}
