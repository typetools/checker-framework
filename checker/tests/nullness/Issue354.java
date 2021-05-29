public class Issue354 {

  String a;

  {
    Object o =
        new Object() {
          @Override
          public String toString() {
            // :: error: (dereference.of.nullable)
            return a.toString();
          }
        }.toString();

    // This is needed to avoid the initialization.fields.uninitialized warning.
    // The NPE still occurs
    a = "";
  }
}
