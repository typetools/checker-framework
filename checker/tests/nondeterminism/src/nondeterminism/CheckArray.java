package nondeterminism;

import org.checkerframework.checker.nondeterminism.qual.*;

public class CheckArray {
	void createArray() {
		@ValueNonDet int[] a = new @ValueNonDet int[9];
		@Det int[] b = new @Det int[9];
		@OrderNonDet int[] c = new @OrderNonDet int[9];
	}
}
