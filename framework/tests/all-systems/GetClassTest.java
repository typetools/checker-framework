class GetClassTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceive

    void context() {
        Integer i = 4;
        i.getClass();
        Class<?> a = i.getClass();
        Class<? extends Object> b = i.getClass();
        Class<? extends Integer> c = i.getClass();
    }
}
