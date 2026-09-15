package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

/**
 * The annotated method destructs the given resource collection fields. That is, this method calls
 * all required methods on their elements.
 *
 * <pre><code>
 *  {@literal @}CollectionFieldDestructor("socketList")
 *  void close() {
 *    for (Socket s : this.socketList) {
 *      try {
 *        s.close();
 *      } catch (Exception e) {}
 *    }
 *  }
 * </code></pre>
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@PostconditionAnnotation(qualifier = OwningCollectionWithoutObligation.class)
@InheritedAnnotation
public @interface CollectionFieldDestructor {
  /**
   * Returns the resource collection fields whose collection obligation the destructor fulfills.
   *
   * @return the resource collection fields whose collection obligation the destructor fulfills
   */
  String[] value();
}
