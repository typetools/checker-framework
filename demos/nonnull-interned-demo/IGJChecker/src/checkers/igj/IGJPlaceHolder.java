package checkers.igj;

import checkers.metaquals.TypeQualifier;

/**
 * An annotation used to represent a place holder immutability type, that is a
 * subtype of all other types. For example, {@code null} type is a subtype
 * of all immutability types.
 * 
 * However, it is an implementation detail; hence, the package-scope.
 */
@TypeQualifier
@interface IGJPlaceHolder {

}
