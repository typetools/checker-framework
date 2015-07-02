import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.reflection.qual.MethodVal;

/**
 * Example used in the reflection resolution section of the Checker Framework manual
 * @author smillst
 *
 */
public class NullnessReflectionExampleTest {
    @NonNull Location getCurrentLocation() {
        //...
        return new Location();
    }

    String getCurrentCity() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        @MethodVal(className="NullnessReflectionExampleTest", methodName="getCurrentLocation", params=0)
        Method toLowerCase = getClass().getMethod("getCurrentLocation");
        Location currentLocation = (Location) toLowerCase.invoke(this);
        return currentLocation.nameOfCity();
    }
}

class Location {
   String nameOfCity() { return "Seattle"; }
}
