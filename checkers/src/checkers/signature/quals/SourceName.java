package checkers.signature.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@SubtypeOf({BinaryName.class,
            FullyQualifiedName.class
           })
public @interface SourceName {}
