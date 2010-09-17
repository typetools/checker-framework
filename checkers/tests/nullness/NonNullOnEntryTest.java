import checkers.nullness.quals.*;

/*
 * @skip-test
 */
class NonNullOnEntryTest {

	@Nullable Object field1;
	@Nullable Object field2;

	@NonNullOnEntry("field1")
	void method1() {
		field1.toString(); // OK, field1 is known to be non-null
		//:: (dereference.of.nullable)
		field2.toString(); // error, might throw NullPointerException
	}

	void method2() {
		field1 = new Object();
		method1(); // OK, satisfies method precondition
		field1 = null;
		// XXX TODO FIXME:
		//:: (nonnull.precondition.not.satisfied)
		method1(); // error, does not satisfy method precondition
	}

	private @Nullable Object field;

	@NonNullOnEntry("field")
	public void requiresNonNullField() {}

	public void clientFail(NonNullOnEntryTest arg) {
		// XXX TODO FIXME:
		//:: (nonnull.precondition.not.satisfied)
		arg.requiresNonNullField();
	}

	public void clientOK(NonNullOnEntryTest arg) {
		arg.field = new Object();
		// note that the following line works
		@NonNull Object o = arg.field;
		
		arg.requiresNonNullField(); // OK, field is known to be non-null
	}

	// TODO: forbid the field in @NNOE to be less visible than the method
}
