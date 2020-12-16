package com.example;

import org.checkerframework.checker.nullness.qual.*;

public class GenericTest11 {
    public void m(BeanManager beanManager) {
        Bean<?> bean = beanManager.getBeans(GenericTest11.class).iterator().next();
        CreationalContext<?> context = beanManager.createCreationalContext(bean);
    }

    static interface BeanManager {
        java.util.Set<Bean<?>> getBeans(
                java.lang.reflect.Type arg0, java.lang.annotation.Annotation... arg1);

        <T1> CreationalContext<T1> createCreationalContext(Contextual<T1> arg0);
    }

    static interface Contextual<T2> {}

    static interface Bean<T3> extends Contextual<T3> {}

    static interface CreationalContext<T4> {}
}
