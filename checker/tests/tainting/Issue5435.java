class Issue5435 {
  public @interface A1 {}

  public @interface A2 {
    A1[] m() default {@A1()};
  }
}
