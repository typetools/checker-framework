// To test crash in CFG construction for try-with-resources
// See https://github.com/typetools/checker-framework/issues/6396
import org.apache.spark.sql.SparkSession;

public class SparkSessionCFGCrash {

  private void run() {
    try (SparkSession session = SparkSession.builder().getOrCreate()) {}
  }
}
