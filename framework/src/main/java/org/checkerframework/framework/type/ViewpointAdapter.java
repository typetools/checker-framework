package org.checkerframework.framework.type;

import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * A viewpoint adapter.
 *
 * <p>Viewpoint adaptation applies to member/field accesses, constructor invocations, method
 * invocations, and type parameter bound instantiations.
 */
public interface ViewpointAdapter {

    /**
     * Viewpoint adapts a member/field access.
     *
     * <p>Developer notes: When this method is invoked on a member/field with a type given by a type
     * parameter, the type arguments are correctly substituted, and memberType is already in a good
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
     * Viewpoint adapts a constructor invocation. Takes an unsubstituted method invocation type and
     * performs the viewpoint adaption in place, modifying the parameter.
     *
     * @param receiverType receiver type through which a constructor is invoked.
     * @param constructorElt element of the invoked constructor.
     * @param constructorType invoked type of the constructor with type variables not substituted.
     *     After the method returns, it will be mutated to the viewpoint adapted constructor type.
     */
    void viewpointAdaptConstructor(
            AnnotatedTypeMirror receiverType,
            ExecutableElement constructorElt,
            AnnotatedExecutableType constructorType);

    /**
     * Viewpoint adapts a method invocation. Takes an unsubstituted method invocation type and
     * performs the viewpoint adaption in place, modifying the parameter.
     *
     * @param receiverType receiver type through which a method is invoked.
     * @param methodElt element of the invoked method. Only used to determine whether this type
     *     should be viewpoint adapted
     * @param methodType invoked type of the method with type variables not substituted. After the
     *     method returns, it will be mutated to the viewpoint adapted method type.
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
