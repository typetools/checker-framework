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
 * <p>A call to such a method changes the type of its receiver to {@code @OwningCollection}, and it
 * creates an obligation for each method in the {@code @MustCall} type of the receiver's type
 * argument:
 *
 * <pre><code>
 *  List&lt;Socket&gt; sockets = new ArrayList&lt;&gt;();
 *  // `sockets` is {@literal @}OwningCollectionWithoutObligation
 *  sockets.add(new Socket(host, port));
 *  // `sockets` is {@literal @}OwningCollection, with an obligation to close every element
 * </code></pre>
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
public @interface CreatesCollectionObligation {}
