bless:
	-echo Make sure to run "ant test" in scene-lib before!
	make bless-files

bless-files: TestClassEmpty_Expected.class \
	TestClassNonEmpty_Expected.class \
	TestLocalVariable_Expected.class \
	TestLocalVariableA_Expected.class \
	TestLocalVariableGenericArray_Expected.class \
	TestFieldSimple_Expected.class \
	TestFieldGeneric_Expected.class \
	TestFieldGenericSmall_Expected.class \
	TestTypecast_Expected.class \
	TestTypecastGenericArray_Expected.class \
	TestTypeTest_Expected.class \
	TestObjectCreation_Expected.class \
	TestObjectCreationGenericArray_Expected.class \
	TestMethodReceiver_Expected.class \
	TestMethodReturnTypeGenericArray_Expected.class \


%_Expected.class: %_Generated.class
	-mv $< $@
