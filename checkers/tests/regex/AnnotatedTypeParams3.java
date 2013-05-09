import java.lang.reflect.*;
import java.lang.annotation.Annotation;
import checkers.regex.quals.Regex;

class AnnotatedTypeParams3 {
    private <T extends Annotation> T
            safeGetAnnotation(Field f, Class<T> annotationClass) {
        T annotation;
        try {
            annotation = f.getAnnotation( (Class<T>) annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    private <T extends Annotation> T
    safeGetAnnotation2(Field f, Class<T> annotationClass) {
        T annotation;
        try {
            annotation = f.getAnnotation(annotationClass);
        } catch (Exception e) {
            annotation = null;
        }
        return annotation;
    }

    <@Regex T> void test(T p) {
        Object o = p;
        @Regex Object re = o;
    }

    <T extends @Regex Object> void test2(T p) {
        Object o = p;
        @Regex Object re = o;
    }

    // TODO: do we want to infer the type variable annotation on local variable "o"?
    <T> void test3(@Regex T p) {
        T o = p;
        @Regex T re = o;
    }

}

class OuterClass<E> {
    public InnerClass<E> method() {
        return new InnerClass<E>();
    }

    class InnerClass<A extends E> {}
}