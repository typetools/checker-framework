import org.checkerframework.checker.nullness.qual.*;

// The return of a lambda is a lambda

interface ConsumerSupplier {
  ConsumerR get();
}

interface ConsumerR {
  void method(@Nullable String s);
}

interface SupplierSupplier {
  SupplierRe get();
}

interface SupplierRe {
  @NonNull String method();
}

class MetaReturn {

  // :: error: (dereference.of.nullable)
  ConsumerSupplier t1 = () -> (s) -> s.toString();
  ConsumerSupplier t2 =
      () -> {
        // :: error: (lambda.param)
        return (String s) -> {
          s.toString();
        };
      };

  SupplierSupplier t3 =
      () -> {
        // :: error: (return)
        return () -> null;
      };

  SupplierSupplier t4 =
      () -> {
        return ""::toString;
      };
}
