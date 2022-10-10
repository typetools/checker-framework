package org.checkerframework.checker.mustcall;

import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;

import javax.lang.model.element.ExecutableElement;

/**
 * This interface should be implemented by all type factories that can provide an {@link
 * ExecutableElement} for {@link CreatesMustCallFor} and {@link CreatesMustCallFor.List}. This
 * interface is needed so any type factory with these elements can be used to retrieve information
 * about these annotations, not just the MustCallAnnotatedTypeFactory (in particular, the
 * consistency checker needs to be able to call that method with both the CalledMethods type factory
 * and the MustCall type factory).
 */
public interface CreatesMustCallForElementSupplier {

    /**
     * Returns the CreatesMustCallFor.value field/element.
     *
     * @return the CreatesMustCallFor.value field/element
     */
    ExecutableElement getCreatesMustCallForValueElement();

    /**
     * Returns the CreatesMustCallFor.List.value field/element.
     *
     * @return the CreatesMustCallFor.List.value field/element
     */
    ExecutableElement getCreatesMustCallForListValueElement();
}
