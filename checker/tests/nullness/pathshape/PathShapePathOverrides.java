// Tests that an implementation of a method in Path is treated like the method that it implements,
// whether it is declared in the Path subtype or inherited from a class that is not a Path.

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.HasName;
import org.checkerframework.checker.nullness.qual.HasParent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathShapePathOverrides {

  void declared(@HasParent PathShapeDeclaredPath parent, @HasName PathShapeDeclaredPath name) {
    @NonNull Path p1 = parent.getParent();
    @NonNull Path p2 = name.getFileName();
    @NonNull Path p3 = name.toAbsolutePath().getParent();
    // :: error: (assignment)
    @NonNull Path p4 = name.getParent();
  }

  void inherited(@HasParent PathShapeInheritedPath parent, @HasName PathShapeInheritedPath name) {
    @NonNull Path p1 = parent.getParent();
    @NonNull Path p2 = name.getFileName();
    @NonNull Path p3 = name.toAbsolutePath().getParent();
    // :: error: (assignment)
    @NonNull Path p4 = name.getParent();
  }

  void notAPath(PathShapePathBase base) {
    // :: error: (assignment)
    @NonNull Path p1 = base.getParent();
  }
}

abstract class PathShapeDeclaredPath implements Path {
  @Override
  public @Nullable Path getParent() {
    return null;
  }

  @Override
  public @Nullable Path getFileName() {
    return null;
  }

  @Override
  public Path toAbsolutePath() {
    return this;
  }
}

/** A class that is not a Path, but whose methods implement Path's methods in a subclass. */
abstract class PathShapePathBase {
  public @Nullable Path getParent() {
    return null;
  }

  public @Nullable Path getFileName() {
    return null;
  }

  public Path toAbsolutePath() {
    return Path.of("/");
  }
}

abstract class PathShapeInheritedPath extends PathShapePathBase implements Path {}
