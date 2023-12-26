import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

public class UncheckedCrash {
  void method(AnnotationMirror anno, ExecutableElement ele) {
    @SuppressWarnings("unchecked")
    List<AnnotationMirror> values = getElementValue(anno, ele, List.class);
  }

  public static <T> T getElementValue(
      AnnotationMirror anno, ExecutableElement element, Class<T> expectedType) {
    throw new RuntimeException();
  }
}
