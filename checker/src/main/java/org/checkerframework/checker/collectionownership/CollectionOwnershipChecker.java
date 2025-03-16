package org.checkerframework.checker.collectionownership;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * This typechecker tracks at most one owning variable per resource collection using an ownership
 * type system. The resource leak checker verifies that the determined owner fulfills the calling
 * obligations of its elements.
 */
public class CollectionOwnershipChecker extends BaseTypeChecker {}
