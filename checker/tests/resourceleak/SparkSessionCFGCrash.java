// To test crash in CFG construction for try-with-resources
// See https://github.com/typetools/checker-framework/issues/6396

import java.io.IOException;
import org.apache.spark.sql.SparkSession;

public class SparkSessionCFGCrash {

  private void run() throws IOException {
    try (SparkSession session = SparkSession.builder().getOrCreate()) {}
  }
}
