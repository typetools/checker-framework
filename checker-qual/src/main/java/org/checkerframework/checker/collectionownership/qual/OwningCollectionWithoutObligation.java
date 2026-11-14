package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @OwningCollectionWithoutObligation} is a resource collection/array
 * that definitely owns the underlying collection/array. Furthermore, all of the methods in the
 * {@code @MustCall} type of every element have already been called on that element.
 *
 * <p>For example, a freshly allocated collection has this type, and so does a collection after a
 * loop that calls the required methods on all of its elements:
 *
 * <pre><code>
 *  List&lt;Socket&gt; sockets = new ArrayList&lt;&gt;();
 *  // `sockets` is {@literal @}OwningCollectionWithoutObligation
 *  sockets.add(new Socket(host, port));
 *  // `sockets` is {@literal @}OwningCollection
 *  for (Socket s : sockets) {
 *    try {
 *      s.close();
 *    } catch (IOException e) {}
 *  }
 *  // `sockets` is {@literal @}OwningCollectionWithoutObligation
 * </code></pre>
 *
 * <p>A destructor method that is annotated with {@link CollectionFieldDestructor} ensures that the
 * given fields have this type when the method exits.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({OwningCollection.class})
public @interface OwningCollectionWithoutObligation {}
