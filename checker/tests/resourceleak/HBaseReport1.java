// Based on a HBase false positive
// In this example fstream is @MCA with out and close is called on all
// exit paths but we randomly report a warning for this test case
// @skip-test

import java.io.*;
import java.util.*;

class HBaseReport1 {

  public static void test(String fileName) {
    FileWriter fstream;
    try {
      // :: error: required.method.not.called
      fstream = new FileWriter(fileName);
    } catch (IOException e) {
      return;
    }

    BufferedWriter out = new BufferedWriter(fstream);

    try {
      try {
        out.write(fileName + "\n");
      } finally {
        try {
          out.close();
        } finally {
          fstream.close();
        }
      }
    } catch (IOException e) {

    }
  }
}
