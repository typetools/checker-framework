class GetClassTest {

    // See AnntoatedTypeFactory.adaptGetClassReturnTypeToReceiver

    void context() {
        Integer i = 4;
        Class<?> a = i.getClass();
        Class<? extends Object> b = i.getClass();
        Class<? extends Integer> c = i.getClass();
    }
}
