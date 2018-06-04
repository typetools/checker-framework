package org.checkerframework.framework.type;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

/**
 * This interface defines the methods that must be implemented by a viewpoint adaptor utility class.
 *
 * <p>Standard viewpoint adaptation applies to member/field accesses, constructor invocations,
 * method invocations and type parameter instantiations.
 */
public interface ViewpointAdapter {

    /**
     * Viewpoint adapts a member/field access.
     *
     * <p>Developer notes: When this method is invoked on a member/field with a type given by a type
     * parameter. The type arguments are correctly substituted, and memberType is already in a good
     * shape. Only annotations on the memberType should be replaced by the viewpoint adapted ones.
     *
     * @param receiverType receiver type through which the member/field is accessed.
     * @param memberElement element of the accessed member/field.
     * @param memberType accessed type of the member/field. After the method returns, it will be
     *     mutated to the viewpoint adapted result.
     */
    void viewpointAdaptMember(
            AnnotatedTypeMirror receiverType,
            Element memberElement,
            AnnotatedTypeMirror memberType);

    /**
     * Viewpoint adapts a constructor invocation.
     *
     * @param receiverType receiver type through which a constructor is invoked.
     * @param constructorElt element of the invoked constructor.
     * @param constructorType invoked type of the constructor. After the method returns, it will be
     *     mutated to the viewpoint adapted constructor signature.
     */
    void viewpointAdaptConstructor(
            AnnotatedTypeMirror receiverType,
            ExecutableElement constructorElt,
            AnnotatedExecutableType constructorType);

    /**
     * Viewpoint adapts a method invocation.
     *
     * @param receiverType receiver type through which a method is invoked.
     * @param methodElt element of the invoked method.
     * @param methodType invoked type of the method. After the method returns, it will be mutated to
     *     the viewpoint adapted method signature.
     */
    void viewpointAdaptMethod(
            AnnotatedTypeMirror receiverType,
            ExecutableElement methodElt,
            AnnotatedExecutableType methodType);

    /**
     * Viewpoint adapts a type parameter bound when being instantiated.
     *
     * @param receiverType receiver type through which the type parameter is instantiated.
     * @param typeParameterBounds a list of type parameter bounds. After the method returns, it will
     *     be mutated to the viewpoint adapted type parameter bounds.
     */
    void viewpointAdaptTypeParameterBounds(
            AnnotatedTypeMirror receiverType,
            List<AnnotatedTypeParameterBounds> typeParameterBounds);
}
