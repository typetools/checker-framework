package org.checkerframework.checker.collectionownership.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;

/**
 * A method carrying this annotation creates a {@code CollectionObligation} for the receiver
 * collection.
 *
 * <p>Consider a call to a {@code CreatesCollectionObligation}-annotated method. If the receiver is
 * of type {@code @OwningCollectionWithoutObligation}, it is unrefined to {@code @OwningCollection},
 * and a CollectionObligation is created for each {@code @MustCall} method of the type variable of
 * the receiver.
 *
 * <p>This annotation should only be used on method declarations of collections, as
 * defined by the CollectionOwnershipChecker, that is, {@code java.lang.Iterable} and {@code
 * java.util.Iterator} implementations.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Documented
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CreatesCollectionObligation {}
