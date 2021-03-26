import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Subtyping {
  void test1(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    // ::error: (assignment.type.incompatible)
    underObject = unknownObject;
    underObject = underSubtyping;
    // ::error: (assignment.type.incompatible)
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
    // ::error: (assignment.type.incompatible)
    underSubtyping = unknownObject;
    // ::error: (assignment.type.incompatible)
    underSubtyping = unknownSubtyping;
    // ::error: (assignment.type.incompatible)
    underSubtyping = underObject;
  }

  void test4(
      @UnknownInitialization(Object.class) Object unknownObject,
      @UnderInitialization(Object.class) Object underObject,
      @UnknownInitialization(Subtyping.class) Object unknownSubtyping,
      @UnderInitialization(Subtyping.class) Object underSubtyping) {
    // ::error: (assignment.type.incompatible)
    unknownSubtyping = unknownObject;
    unknownSubtyping = underSubtyping;
    // ::error: (assignment.type.incompatible)
    unknownSubtyping = underObject;
  }
}
