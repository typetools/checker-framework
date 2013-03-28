package dataflow.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code TerminatesExecution} is a method annotation that indicates that a
 * method terminates the execution of the program. This can be used to annotate
 * methods such as {@code System.exit()}.
 * 
 * <p>
 * The annotation is a <em>trusted</em> annotation, meaning that it is not
 * checked whether the annotated method really does terminate the program.
 * 
 * @author Stefan Heule
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TerminatesExecution {
}
