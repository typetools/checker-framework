package java.lang.annotation;
import checkers.javari.quals.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {
     RetentionPolicy value();
}
