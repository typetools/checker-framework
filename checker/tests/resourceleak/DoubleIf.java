// Based on an FP in ZK. Only #parse threw an error;
// removing either if statement made the code verifiable.
// Adding an @CalledMethods annotation, like in parse4, also
// makes this code verifiable...

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;

class DoubleIf {

  String fn;

  public void parse(boolean b, boolean c) throws Exception {
    if (c) {
      FileInputStream fis1 = new FileInputStream(fn);
      try {
      } finally {
        fis1.close();
      }
      if (b) {}
    }
  }

  public void parse2(boolean c) throws Exception {
    if (c) {
      FileInputStream fis2 = new FileInputStream(fn);
      try {
      } finally {
        fis2.close();
      }
    }
  }

  public void parse3(boolean b) throws Exception {
    FileInputStream fis3 = new FileInputStream(fn);
    try {
    } finally {
      fis3.close();
    }
    if (b) {}
  }

  public void parse4(boolean b, boolean c) throws Exception {
    if (c) {
      FileInputStream fis4 = new FileInputStream(fn);
      try {
      } finally {
        fis4.close();
      }
      if (b) {}
      @CalledMethods("close") FileInputStream fis24 = fis4;
    }
  }
}
