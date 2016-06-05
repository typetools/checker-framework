class GetClassTest {

    // See AnntoatedTypeFactory.adaptGetClassReturnTypeToReceiver

    //@SuppressWarnings("unsignedness")
    void context() {
        Integer i = 4;
        i.getClass();
        Class<?> a = i.getClass();
        Class<? extends Object> b = i.getClass();
        Class<? extends Integer> c = i.getClass();
    }
}
