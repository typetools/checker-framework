// The test cases GenericNull, FieldAccess, and InferTypeArgs often fail together.
// See the comments at GenericNull for some tips about what might be wrong.

/**
 * This test came from running the compilermsgs checker in the checker-framework/checker directory
 * It's to test the result of type argument inference. We used to have the following
 * return.type.incompatible found: FlowAnalysis[ extends @UnknownPropertyKey
 * CFAbstractAnalysis<Value[ extends @UnknownPropertyKey CFAbstractValue<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value> super @UnknownPropertyKey Void]>
 * super @UnknownPropertyKey Void], Store[ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], TransferFunction[
 * extends @UnknownPropertyKey CFAbstractTransfer<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], TransferFunction[
 * extends @UnknownPropertyKey CFAbstractTransfer<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], Store>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void], TransferFunction>
 * super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void]> super @UnknownPropertyKey Void]
 * required: FlowAnalysis[ extends @UnknownPropertyKey CFAbstractAnalysis<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom Void]>
 * super @Bottom Void], Store[ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store> super @Bottom Void]>
 * super @Bottom Void], TransferFunction[ extends @UnknownPropertyKey CFAbstractTransfer<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom Void]>
 * super @Bottom Void], Store[ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store> super @Bottom Void]>
 * super @Bottom Void], TransferFunction[ extends @UnknownPropertyKey CFAbstractTransfer<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store[
 * extends @UnknownPropertyKey CFAbstractStore<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value[ extends @UnknownPropertyKey CFAbstractValue<Value> super @Bottom Void]>
 * super @Bottom Void], Store[ extends @UnknownPropertyKey CFAbstractStore<Value[
 * extends @UnknownPropertyKey CFAbstractValue<Value[ extends @UnknownPropertyKey
 * CFAbstractValue<Value> super @Bottom Void]> super @Bottom Void], Store> super @Bottom Void]>
 * super @Bottom Void], TransferFunction> super @Bottom Void]> super @Bottom Void]> super @Bottom
 * Void]
 */
class CFAbstractValue<V extends CFAbstractValue<V>> {}

class CFAbstractAnalysis<V extends CFAbstractValue<V>> {}

class GenericAnnotatedTypeFactory<
        Value extends CFAbstractValue<Value>, FlowAnalysis extends CFAbstractAnalysis<Value>> {

    @SuppressWarnings("immutability:type.argument.type.incompatible")
    protected FlowAnalysis createFlowAnalysis() {
        FlowAnalysis result = invokeConstructorFor();
        return result;
    }

    @SuppressWarnings({
        "nullness:return.type.incompatible",
        "lock:return.type.incompatible",
        "immutabilitysub:type.argument.type.incompatible"
    })
    public static <T> T invokeConstructorFor() {
        return null;
    }
}
