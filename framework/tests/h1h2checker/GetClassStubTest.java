package h1h2checker;

import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class GetClassStubTest {

  // See AnnotatedTypeFactory.adaptGetClassReturnTypeToReceiver
  void context() {
    Integer i = 4;
    Class<?> a = i.getClass();

    Class<@H1Bot ? extends @H1S1 Object> succeed1 = i.getClass();
    Class<@H1Bot ? extends @H1S1 Integer> succeed2 = i.getClass();

    // :: error: (assignment)
    Class<@H1Bot ? extends @H1Bot Object> fail1 = i.getClass();

    // :: error: (assignment)
    Class<@H1Bot ?> fail2 = i.getClass();
  }
}
