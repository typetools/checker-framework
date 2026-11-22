package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * The annotated method might insert an element into its receiver collection, and thus creates an
 * obligation for the receiver collection. For example, {@code Collection.add} is annotated with
 * {@code @CreatesCollectionObligation}.
 *
 * <p>A call to such a method on an owning receiver changes the type of the receiver to
 * {@code @OwningCollection}, and it creates an obligation for each method in the {@code @MustCall}
 * type of the receiver's type argument. A receiver of type {@code @NotOwningCollection} keeps that
 * type and gains no obligation, because it can insert only non-owning elements. For example:
 *
 * <pre><code>
 *  List&lt;Socket&gt; sockets = new ArrayList&lt;&gt;();
 *  // `sockets` is {@literal @}OwningCollectionWithoutObligation
 *  sockets.add(new Socket(host, port));
 *  // `sockets` is {@literal @}OwningCollection, with an obligation to close every element
 * </code></pre>
 *
 * <p>The inserted value is the method's last parameter, unless {@link #element} names another one.
 * Its type is the collection's element type, or, for a method such as {@code addAll} that inserts
 * all the elements of another collection, a collection type.
 *
 * <p>This annotation should be written only on methods declared in a subtype of {@code
 * java.lang.Iterable}.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CreatesCollectionObligation {
  /**
   * The parameter whose value is inserted, written as {@code "#1"} for the first parameter, {@code
   * "#2"} for the second, and so on.
   *
   * <p>The default is the last parameter, which is the inserted value for most methods, such as
   * {@code add(E e)} and {@code add(int index, E element)}. A method whose inserted value is not
   * its last parameter must name it. For example, {@code BlockingQueue.offer(E e, long timeout,
   * TimeUnit unit)} is annotated with {@code @CreatesCollectionObligation(element = "#1")}.
   *
   * @return the parameter whose value is inserted, or the empty string for the last parameter
   */
  String element() default "";
}
