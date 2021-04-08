import com.google.auto.value.AutoValue;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.nullness.qual.*;

@AutoValue
abstract class NonAVBuilder {
  abstract String name();

  public Builder toBuilder() {
    return new Builder(this);
  }

  // NOT an AutoValue builder
  static final class Builder {

    Builder(NonAVBuilder b) {}
  }
}
