public class Issue6373 {

  abstract static class C1<
          C extends C1<C, Q, B, D, CR>,
          Q extends C2<C, Q, B, D, CR>,
          B extends C3<C, Q, B, D, CR>,
          D extends C4<C, Q, B, D, CR>,
          CR extends C5<CR>>
      extends C6 {}

  static class C6 {}

  abstract static class C2<
          C extends C1<C, Q, B, D, RT>,
          Q extends C2<C, Q, B, D, RT>,
          B extends C3<C, Q, B, D, RT>,
          D extends C4<C, Q, B, D, RT>,
          RT extends C5<RT>>
      implements C7 {}

  abstract static class C3<
      C extends C1<C, Q, B, D, R>,
      Q extends C2<C, Q, B, D, R>,
      B extends C3<C, Q, B, D, R>,
      D extends C4<C, Q, B, D, R>,
      R extends C5<R>> {}

  abstract static class C4<
      C extends C1<C, Q, B, D, R>,
      Q extends C2<C, Q, B, D, R>,
      B extends C3<C, Q, B, D, R>,
      D extends C4<C, Q, B, D, R>,
      R extends C5<R>> {
    interface I<T> {}
  }

  abstract static class C5<R2 extends C5<R2>> implements C7 {}

  interface C7 {}

  abstract static class C8<
      C extends C1<C, Q, B, D, CR>,
      Q extends C2<C, Q, B, D, CR>,
      B extends C3<C, Q, B, D, CR>,
      D extends C4<C, Q, B, D, CR>,
      CR extends C5<CR>,
      RpT extends C5<RpT>> {

    public static <
            CM extends C1<CM, QM, BM, DM, CRM>,
            QM extends C2<CM, QM, BM, DM, CRM>,
            BM extends C3<CM, QM, BM, DM, CRM>,
            DM extends C4<CM, QM, BM, DM, CRM>,
            CRM extends C5<CRM>,
            RpTM extends C5<RpTM>>
        Builder<CM, QM, BM, DM, CRM, RpTM> n(QM q) {
      throw new AssertionError();
    }

    abstract static class Builder<
        C extends C1<C, Q, B, D, CR>,
        Q extends C2<C, Q, B, D, CR>,
        B extends C3<C, Q, B, D, CR>,
        D extends C4<C, Q, B, D, CR>,
        CR extends C5<CR>,
        RpT extends C5<RpT>> {}
  }

  abstract static class C9<W extends C9<W>> {}

  static class C13 {

    static final class C14 extends C1<C14, C15, C16, C17, C18> {}

    static final class C15 extends C2<C14, C15, C16, C17, C18> {}

    static final class C18 extends C5<C18> {}

    static class C17 extends C4<C14, C15, C16, C17, C18> implements C4.I<Long>, C19 {}

    static final class C16 extends C3<C14, C15, C16, C17, C18> {}
  }

  interface C19 {}

  void f(C13.C15 c) {
    C8.n(c);
  }
}
