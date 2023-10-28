import java.util.Optional;
import org.checkerframework.checker.optional.qual.EnsuresPresentIf;
import org.checkerframework.checker.optional.qual.Present;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class EnsuresPresentIfTest {

  // :: warning: (optional.field)
  private Optional<String> optId = Optional.of("abc");

  @Pure
  public Optional<String> getOptId() {
    return Optional.of("abc");
  }

  @EnsuresPresentIf(result = true, expression = "getOptId()")
  public boolean hasPresentId1() {
    return getOptId().isPresent();
  }

  @EnsuresPresentIf(result = true, expression = "this.getOptId()")
  public boolean hasPresentId2() {
    return getOptId().isPresent();
  }

  @EnsuresQualifierIf(result = true, expression = "getOptId()", qualifier = Present.class)
  public boolean hasPresentId3() {
    return getOptId().isPresent();
  }

  @EnsuresQualifierIf(result = true, expression = "this.getOptId()", qualifier = Present.class)
  public boolean hasPresentId4() {
    return getOptId().isPresent();
  }

  @EnsuresPresentIf(result = true, expression = "optId")
  public boolean hasPresentId5() {
    return optId.isPresent();
  }

  @EnsuresPresentIf(result = true, expression = "this.optId")
  public boolean hasPresentId6() {
    return optId.isPresent();
  }

  @EnsuresQualifierIf(result = true, expression = "optId", qualifier = Present.class)
  public boolean hasPresentId7() {
    return optId.isPresent();
  }

  @EnsuresQualifierIf(result = true, expression = "this.optId", qualifier = Present.class)
  public boolean hasPresentId8() {
    return optId.isPresent();
  }

  void client() {
    if (hasPresentId1()) {
      getOptId().get();
    }
    if (hasPresentId2()) {
      getOptId().get();
    }
    if (hasPresentId3()) {
      getOptId().get();
    }
    if (hasPresentId4()) {
      getOptId().get();
    }
    if (hasPresentId5()) {
      optId.get();
    }
    if (hasPresentId6()) {
      optId.get();
    }
    if (hasPresentId7()) {
      optId.get();
    }
    if (hasPresentId8()) {
      optId.get();
    }
    // :: error: (method.invocation)
    optId.get();
  }
}
