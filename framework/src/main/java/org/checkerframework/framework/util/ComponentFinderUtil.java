package org.checkerframework.framework.util;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Util class to find components (e.g. visitors and factories) following the naming convention
 * reflectively. For example, ABCChecker's default naming for visitor is "ABCVisitor". If a visitor
 * class with that name exists, then instantiate it and returns. Otherwise try to find the super,
 * and finally uses the {@code defaultGetter} callback to get a default value if all attempt fails.
 */
public class ComponentFinderUtil {

    /**
     * A single method interface for getting default type for components by method reference.
     *
     * @param <DefaultType> the type for the default value
     */
    public interface DefaultGetter<DefaultType> {

        /**
         * The logic for getting the default value if all attempt fails
         *
         * @param checker a checker as the argument of constructor
         * @return the default value
         */
        DefaultType getDefault(BaseTypeChecker checker);
    }

    /**
     * Find a component named with the checker naming convention.
     *
     * @param <T> type of the component
     * @param checker the current checker
     * @param replacement the component suffix, e.g. "Visitor" or "AnnotatedTypeFactory"
     * @param defaultGetter a getter for the default value when cannot find the class
     * @param constructorParamTypes parameter types passed to constructor of the target class
     * @param constructorArgs arguments passed to constructor of the target class
     * @return the properly-named component found
     */
    @SuppressWarnings("unchecked")
    public static <T> T find(
            BaseTypeChecker checker,
            String replacement,
            DefaultGetter<T> defaultGetter,
            Class<?>[] constructorParamTypes,
            Object[] constructorArgs) {
        // Try to reflectively load the component.
        Class<?> checkerClass = checker.getClass();
        while (checkerClass != BaseTypeChecker.class) {
            T result =
                    (T)
                            BaseTypeChecker.invokeConstructorFor(
                                    BaseTypeChecker.getRelatedClassName(checkerClass, replacement),
                                    constructorParamTypes,
                                    constructorArgs);
            if (result != null) {
                return result;
            }
            checkerClass = checkerClass.getSuperclass();
        }
        return defaultGetter.getDefault(checker);
    }

    /**
     * Find a component named with the checker naming convention with its constructor invoked with
     * the only argument {@code checker}
     *
     * @param <T> type of the component
     * @param checker a checker
     * @param replacement the component suffix, e.g. "Visitor" or "AnnotatedTypeFactory"
     * @param defaultGetter a getter for the default value when cannot find the class
     * @return the properly-named component found
     */
    public static <T> T findAndInitWithChecker(
            BaseTypeChecker checker, String replacement, DefaultGetter<T> defaultGetter) {
        return find(
                checker,
                replacement,
                defaultGetter,
                new Class<?>[] {BaseTypeChecker.class},
                new Object[] {checker});
    }
}
