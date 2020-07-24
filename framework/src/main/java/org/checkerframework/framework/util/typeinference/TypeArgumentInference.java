package org.checkerframework.framework.util.typeinference;

import com.sun.source.tree.ExpressionTree;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * Instances of TypeArgumentInference are used to infer the types of method type arguments when no
 * explicit arguments are provided.
 *
 * <p>e.g. If we have a method declaration:
 *
 * <pre>{@code
 * <A,B> B method(A a, B b) {...}
 * }</pre>
 *
 * And an invocation of that method:
 *
 * <pre>{@code
 * method("some Str", 35);
 * }</pre>
 *
 * TypeArgumentInference will determine what the type arguments to type parameters A and B are. In
 * Java, if T(A) = the type argument for a, in the above example T(A) == String and T(B) == Integer
 *
 * <p>For the Checker Framework we also need to infer reasonable annotations for these type
 * arguments. For information on inferring type arguments see the documentation in JLS7 and JLS8:
 * https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html
 * https://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.12.2.7
 *
 * <p>Note: It appears that Java 8 greatly improved the type argument inference and related error
 * messaging but I have found it useful to consult the JLS 7 as well.
 */
public interface TypeArgumentInference {

    /**
     * Infer the type arguments for the method or constructor invocation given by invocation.
     *
     * @param typeFactory the type factory used to create methodType
     * @param invocation a tree representing the method or constructor invocation for which we are
     *     inferring type arguments
     * @param methodElem the element for the declaration of the method being invoked
     * @param methodType the declaration type of method elem
     * @return a mapping between the Java type parameter and the annotated type that was inferred
     *     for it. Note: We use the Java TypeVariable type because this uniquely identifies a
     *     declaration where as two uses of an AnnotatedTypeVariable may be uses of the same
     *     declaration but are not .equals to each other.
     */
    public Map<TypeVariable, AnnotatedTypeMirror> inferTypeArgs(
            final AnnotatedTypeFactory typeFactory,
            final ExpressionTree invocation,
            final ExecutableElement methodElem,
            final AnnotatedExecutableType methodType);
}
