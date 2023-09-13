// @below-java21-jdk-skip-test
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
