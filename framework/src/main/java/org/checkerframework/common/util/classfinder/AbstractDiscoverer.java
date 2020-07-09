package org.checkerframework.common.util.classfinder;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Enables the auto discovery of classes following the naming convention.
 *
 * @param <T>
 */
public abstract class AbstractDiscoverer<T> {

    /**
     * As the name implies, this interface is used to get the default value
     *
     * @param <DefaultType> the type of the default value
     */
    public interface DefaultGetter<DefaultType> {

        /**
         * get the default value
         *
         * @param checker a checker as the argument of constructor
         * @return the default value
         */
        DefaultType getDefault(BaseTypeChecker checker);
    }

    /**
     * Find a properly-named component.
     *
     * @param checker the current checker
     * @param replacement the component suffix, e.g. "Visitor" or "AnnotatedTypeFactory"
     * @param defaultGetter a getter for the default value when cannot find the class
     * @param constructorParamTypes parameter types passed to constructor of the target class
     * @param constructorArgs arguments passed to constructor of the target class
     * @return the properly-named component found
     */
    public abstract T find(
            BaseTypeChecker checker,
            String replacement,
            DefaultGetter<T> defaultGetter,
            Class<?>[] constructorParamTypes,
            Object[] constructorArgs);

    /**
     * Find a properly-named component with its constructor invoked with the only argument {@code
     * checker}
     *
     * @param checker a checker
     * @param replacement the component suffix, e.g. "Visitor" or "AnnotatedTypeFactory"
     * @param defaultGetter a getter for the default value when cannot find the class
     * @return the properly-named component found
     */
    public T findAndInitWithChecker(
            BaseTypeChecker checker, String replacement, DefaultGetter<T> defaultGetter) {
        return find(
                checker,
                replacement,
                defaultGetter,
                new Class<?>[] {BaseTypeChecker.class},
                new Object[] {checker});
    }
}
