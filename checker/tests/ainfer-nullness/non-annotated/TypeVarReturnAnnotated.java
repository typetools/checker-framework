// Code like this caused WPI to loop infinitely, because the annotation on the return type
// was only sometimes inferred. Based on an example from
// https://github.com/dd482IT/cache2k-wpi/blob/0eaa156bdecd617b2aa4c745d0f8844a32609697/cache2k-api/src/main/java/org/cache2k/config/ToggleFeature.java#L73

public class TypeVarReturnAnnotated {
  public static <T extends TypeVarReturnAnnotated> T extract() {
    return null;
  }
}
