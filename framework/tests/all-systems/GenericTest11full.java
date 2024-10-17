public class GenericTest11full {

  static interface BeanManager {
    java.util.Set<Bean<?>> getBeans(
        java.lang.reflect.Type arg0, java.lang.annotation.Annotation... arg1);

    <T> CreationalContext<T> createCreationalContext(Contextual<T> arg0);

    Object getReference(Bean<?> arg0, java.lang.reflect.Type arg1, CreationalContext<?> arg2);
  }

  static interface Contextual<T> {}

  static interface Bean<T> extends Contextual<T> {}

  static interface CreationalContext<T> {}
}
