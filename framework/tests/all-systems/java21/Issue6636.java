// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import java.util.function.Function;

public class Issue6636 {
  interface Decorator {
    <In, Out> Function<In, Out> decorate(Function<? super In, ? extends Out> target);
  }

  interface Targets {
    <In, Out> Function<In, Out> get(Class<In> in, Class<Out> out);
  }

  record Request() {}

  record Response() {}

  public Response call(Decorator decorator, Targets targets, Request request) {
    return decorator.decorate(targets.get(Request.class, Response.class)).apply(request);
  }
}
