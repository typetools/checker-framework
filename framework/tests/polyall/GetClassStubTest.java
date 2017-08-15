package polyall;

import polyall.quals.*;

class GetClassStubTest {

    // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver
    void context() {
        Integer i = 4;
        Class<?> a = i.getClass();

        Class<@H1Bot ? extends @H1S1 Object> succeed1 = i.getClass();
        Class<@H1Bot ? extends @H1S1 Integer> succeed2 = i.getClass();

        //:: error: (assignment.type.incompatible)
        Class<@H1Bot ? extends @H1Bot Object> fail1 = i.getClass();

        //:: error: (assignment.type.incompatible)
        Class<@H1Bot ?> fail2 = i.getClass();
    }
}
