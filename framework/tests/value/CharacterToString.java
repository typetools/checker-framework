// Annotation on the return value of Character.toString caused
// two annotations from the same hierarchy.
// https://github.com/typetools/checker-framework/issues/1356
public class CharacterToString {
  void m() {
    String s = Character.toString('a');
  }
}
