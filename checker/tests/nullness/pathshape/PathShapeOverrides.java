// Tests that an override of a method in File is treated like the method that it overrides.

import java.io.File;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapeOverrides {

  void getParent(@HasParent PathShapeMyFile parent, @HasName PathShapeMyFile name) {
    @NonNull File f1 = parent.getParentFile();
    @NonNull String s1 = parent.getParent();
    @NonNull File f2 = name.getAbsoluteFile().getParentFile();
    // :: error: (assignment)
    @NonNull File f3 = name.getParentFile();
  }

  void isFile(PathShapeMyFile f) {
    if (f.isFile()) {
      f.getAbsoluteFile().getParentFile().toString();
    }
    // :: error: (dereference.of.nullable)
    f.getAbsoluteFile().getParentFile().toString();
  }
}

class PathShapeMyFile extends File {
  PathShapeMyFile(String s) {
    super(s);
  }

  @Override
  public @Nullable File getParentFile() {
    return super.getParentFile();
  }

  @Override
  public @Nullable String getParent() {
    return super.getParent();
  }

  @Override
  public File getAbsoluteFile() {
    return super.getAbsoluteFile();
  }

  @Override
  public boolean isFile() {
    return super.isFile();
  }
}
