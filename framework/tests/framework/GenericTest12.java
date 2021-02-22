import java.lang.annotation.Annotation;

public class GenericTest12 {
    @interface Anno {}

    void foo(Class<? extends Annotation> qual) {
        Annotation a = qual.getAnnotation(Anno.class);
        Anno an = qual.getAnnotation(Anno.class);
    }
}
