import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.checkerframework.checker.serialization.qual.Sensitive;

/** Test case for rule SER03-J: "Warning for non-transient fields annotated Sensitive" */
public class Coordinates {

  public static class Point implements Serializable {
    @Sensitive private double x;
    @Sensitive private double y;

    public Point(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public Point() {
      // No-argument constructor
    }
  }
  ;

  public static void main(String[] args) {
    FileOutputStream fout = null;
    try {
      Point p = new Point(5, 2);
      fout = new FileOutputStream("point.ser");
      ObjectOutputStream oout = new ObjectOutputStream(fout);
      // :: warning: (warning.sensitive)
      oout.writeObject(p);
    } catch (Throwable t) {
      // Forward to handler
    } finally {
      if (fout != null) {
        try {
          fout.close();
        } catch (IOException x) {
          // Handle error
        }
      }
    }
  }
}
