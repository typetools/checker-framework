import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;

@SuppressWarnings("all")
public class Issue6421C {
  private static String[] getParameterNames(Constructor<?> constructor) {
    Parameter[] parameters = constructor.getParameters();
    return Arrays.stream(parameters).map(Parameter::getName).toArray(String[]::new);
  }

  public void processConstructor(Stream<Class<?>> stream2) {
    Class<?>[] arguments = stream2.toArray(Class[]::new);
  }
}
