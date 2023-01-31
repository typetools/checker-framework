import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.common.returnsreceiver.qual.This;

@Builder(builderClassName = "BaseBuilder")
@Value
public class LombokBuilderSubclassExample {

  @NonNull Integer attribute;

  public static LombokBuilderSubclassExampleBuilder builder() {
    return new LombokBuilderSubclassExampleBuilder();
  }

  public static class LombokBuilderSubclassExampleBuilder extends BaseBuilder {

    @Override
    @This public LombokBuilderSubclassExampleBuilder attribute(@NonNull Integer attribute) {
      return (LombokBuilderSubclassExampleBuilder) super.attribute(attribute);
    }

    @Override
    public LombokBuilderSubclassExample build(
        @CalledMethods("attribute") LombokBuilderSubclassExampleBuilder this) {
      final LombokBuilderSubclassExample result = super.build();
      // here result.getAttribute() is guaranteed to be non null, so we do not have to check
      // this ourselves

      if (result.getAttribute() < 0) {
        throw new IllegalArgumentException("attribute must be >= 0");
      }

      return result;
    }
  }
}
