import org.checkerframework.checker.signature.qual.*;

public class Conversion {

    class CharChar {
        @InternalForm String binaryNameToInternalForm(@BinaryName String bn) {
            return bn.replace('.', '/');
        }

        @BinaryName String internalFormToBinaryName(@InternalForm String iform) {
            return iform.replace('/', '.');
        }

        @InternalForm String binaryNameToInternalFormWRONG1(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace('/', '.');
        }

        @InternalForm String binaryNameToInternalFormWRONG2(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace(':', '/');
        }

        @InternalForm String binaryNameToInternalFormWRONG3(String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace('.', '/');
        }

        @BinaryName String internalFormToBinaryNameWRONG1(@InternalForm String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace('.', '/');
        }

        @BinaryName String internalFormToBinaryNameWRONG2(@InternalForm String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace('/', ':');
        }

        @BinaryName String internalFormToBinaryNameWRONG3(String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace('/', '.');
        }

        @DotSeparatedIdentifiers String binaryNameToDotSeparatedIdentifiers(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace('$', '.');
        }

        @FullyQualifiedName String binaryNameToFullyQualifiedName(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace('$', '.');
        }
    }

    class CharSequenceCharSequence {
        @InternalForm String binaryNameToInternalForm(@BinaryName String bn) {
            return bn.replace(".", "/");
        }

        @BinaryName String internalFormToBinaryName(@InternalForm String iform) {
            return iform.replace("/", ".");
        }

        @InternalForm String binaryNameToInternalFormWRONG1(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace("/", ".");
        }

        @InternalForm String binaryNameToInternalFormWRONG2(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace(":", "/");
        }

        @InternalForm String binaryNameToInternalFormWRONG3(String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace(".", "/");
        }

        @BinaryName String internalFormToBinaryNameWRONG1(@InternalForm String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace(".", "/");
        }

        @BinaryName String internalFormToBinaryNameWRONG2(@InternalForm String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace("/", ":");
        }

        @BinaryName String internalFormToBinaryNameWRONG3(String iform) {
            // :: error: (return.type.incompatible)
            return iform.replace("/", ".");
        }

        @DotSeparatedIdentifiers String binaryNameToDotSeparatedIdentifiers(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace("$", ".");
        }

        @FullyQualifiedName String binaryNameToFullyQualifiedName(@BinaryName String bn) {
            // :: error: (return.type.incompatible)
            return bn.replace("$", ".");
        }
    }
}
