package com.example;

public class GenericTest11 {
    public void m(BeanManager beanManager) {
        Bean<?> bean = beanManager.getBeans(GenericTest11.class).iterator().next();
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
    }

    static interface BeanManager {
        java.util.Set<Bean<?>> getBeans(
                java.lang.reflect.Type arg0, java.lang.annotation.Annotation... arg1);

        <T> CreationalContext<T> createCreationalContext(Contextual<T> arg0);
    }

    static interface Contextual<T> {}

    static interface Bean<T> extends Contextual<T> {}

    static interface CreationalContext<T> {}
}
