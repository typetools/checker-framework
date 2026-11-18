// Tests the stub file sound-file-topath.astub, which makes File.toPath() return @UnknownPathShape.

import java.io.File;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;

public class SoundFileToPath {

  void toPath(@HasParent File f) {
    // `new File("//server/share")` has a parent, but on Windows its toPath() is a root.
    // :: error: (assignment)
    @HasName Path p = f.toPath();
    // :: error: (dereference.of.nullable)
    new File("//server/share").toPath().getParent().toString();
  }

  void toFile(@HasParent Path p) {
    @HasParent File f = p.toFile();
  }
}
