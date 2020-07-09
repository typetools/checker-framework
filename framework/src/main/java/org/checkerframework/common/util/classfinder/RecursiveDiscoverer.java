package org.checkerframework.common.util.classfinder;

import org.checkerframework.common.basetype.BaseTypeChecker;

public class RecursiveDiscoverer<T> extends AbstractDiscoverer<T> {

    @Override
    @SuppressWarnings("unchecked")
    public T find(
            BaseTypeChecker checker,
            String replacement,
            DefaultGetter<T> defaultGetter,
            Class<?>[] constructorParamTypes,
            Object[] constructorArgs) {
        // Try to reflectively load the type factory.
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
}
