import java.util.Optional;
import org.checkerframework.checker.optional.qual.MaybePresent;
import org.checkerframework.checker.optional.qual.OptionalBottom;
import org.checkerframework.checker.optional.qual.Present;

/** Basic test of subtyping. */
public class SubtypeCheck {

  @SuppressWarnings("optional.parameter")
  void foo(
      @MaybePresent Optional<String> mp,
      @Present Optional<String> p,
      @OptionalBottom Optional<String> ob) {
    @MaybePresent Optional<String> mp2 = mp;
    @MaybePresent Optional<String> mp3 = p;
    @MaybePresent Optional<String> mp4 = ob;
    // :: error: assignment.type.incompatible
    @Present Optional<String> p2 = mp;
    @Present Optional<String> p3 = p;
    @Present Optional<String> p4 = ob;
    // :: error: assignment.type.incompatible
    @OptionalBottom Optional<String> ob2 = mp;
    // :: error: assignment.type.incompatible
    @OptionalBottom Optional<String> ob3 = p;
    @OptionalBottom Optional<String> ob4 = ob;
  }
}
