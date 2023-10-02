// @below-java21-jdk-skip-test

// None of the WPI formats support the new Java 21 languages features, so skip inference until they do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

public class Issue6173 {

  static Object toGroupByQueryWithExtractor2(GroupBy groupBy) {
    return switch (groupBy) {
      case OWNER -> new Object();
      case CHANNEL -> new Object();
      case TOPIC -> new Object();
      case TEAM -> new Object();
      case null -> throw new IllegalArgumentException("GroupBy parameter is required");
    };
  }

  public enum GroupBy {
    OWNER,
    CHANNEL,
    TOPIC,
    TEAM;
  }
}
