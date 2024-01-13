import org.apache.spark.sql.SparkSession;

public class SparkSessionCFGCrash {

  private X run() {
    try (SparkSession session = SparkSession.builder().getOrCreate()) {
      X x = new X(session);
      return x;
    }
  }

  static class X {
    X(SparkSession session) {}
  }
}
