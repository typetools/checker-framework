class GetClassTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver

    void context() {
        Integer i = 4;
        i.getClass();
        Class<?> a = i.getClass();
        // Type agruments don't match
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Object> b = i.getClass();
        // Type agruments don't match
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Integer> c = i.getClass();
    }
}
