/*
 * @test
 * @summary An annotation element value that is out of range for the element's declared type is
 * diagnosed rather than silently truncated.
 *
 * @compile annotationvaluerange/Annotated.java annotationvaluerange/ExampleAnno.java
 * @compile/ref=AnnotationValueRange.goal -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.common.value.ValueChecker annotationvaluerange/Client.java -Astubs=annotationvaluerange/
 */

public class AnnotationValueRangeDriver {}
