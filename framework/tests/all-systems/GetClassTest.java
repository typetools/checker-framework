import java.util.Date;

class GetClassTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver

    void context() {
        Integer i = 4;
        i.getClass();
        Class<?> a = i.getClass();
        // Type arguments don't match
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Object> b = i.getClass();
        // Type arguments don't match
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Integer> c = i.getClass();

        Class<?> d = i.getClass();
        // not legal Java; that is, does not type-check under Java rules
        // Class<Integer> e = i.getClass();
    }

    void m(Date d) {
        @SuppressWarnings("fenum:assignment.type.incompatible")
        Class<? extends Date> c = d.getClass();
    }
}
