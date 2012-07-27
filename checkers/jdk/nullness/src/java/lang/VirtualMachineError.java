package java.lang;

import checkers.nullness.quals.Nullable;

abstract public
class VirtualMachineError extends Error {
    public VirtualMachineError() {
	super();
    }

    public VirtualMachineError(@Nullable String s) {
	super(s);
    }
}
