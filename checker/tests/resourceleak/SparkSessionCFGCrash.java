import org.apache.spark.sql.SparkSession;

public class SparkSessionCFGCrash {

  private void run() {
    try (SparkSession session = SparkSession.builder().getOrCreate()) {}
  }
}
