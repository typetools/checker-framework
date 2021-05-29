@SuppressWarnings("all") // Just check for crashes.
abstract class Issue4083 {
  abstract void use(Predicate<Annotation> predicate);

  void go() {
    use(Annotation::b);
  }

  @interface Annotation {
    boolean b() default false;
  }

  interface Predicate<T> {
    boolean apply(T t);
  }
}
