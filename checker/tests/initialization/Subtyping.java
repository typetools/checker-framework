import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Subtyping {
  void test1(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    // ::error: (assignment)
    underObject = unknownObject;
    underObject = underSubtyping;
    // ::error: (assignment)
    underObject = unknownSubtyping;
  }

  void test2(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    unknownObject = underSubtyping;
    unknownObject = unknownSubtyping;
    unknownObject = underObject;
  }

  void test3(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    // ::error: (assignment)
    underSubtyping = unknownObject;
    // ::error: (assignment)
    underSubtyping = unknownSubtyping;
    // ::error: (assignment)
    underSubtyping = underObject;
  }

  void test4(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    // ::error: (assignment)
    unknownSubtyping = unknownObject;
    unknownSubtyping = underSubtyping;
    // ::error: (assignment)
    unknownSubtyping = underObject;
  }
}
