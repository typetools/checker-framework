package middlepkg;

import otherpkg.PackagePrivateMemberTypes;

// This class exists so that a class in package `otherpkg` can extend, indirectly, another class in
// package `otherpkg`, with a class in a different package between them.  It is used by
// `otherpkg/NotInheritedThroughOtherPackage.java`.
public class Middle extends PackagePrivateMemberTypes {}
