import java.lang.annotation.Annotation;

@interface Anno {}

public class Annotations {
    void takeAnnotation(Annotation a) {}

    // test that a Tree works (source for Anno is in same compilation unit)
    void takeTree(Anno a1) {
        takeAnnotation(a1);
    }

    // test that an Element works (annotation only available from class file)
    void takeElem(SuppressWarnings a2) {
        takeAnnotation(a2);
    }
}
