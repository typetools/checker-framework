package org.checkerframework.checker.nullness;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.SupportedOptions;

/** A concrete instantiation of {@link AbstractNullnessChecker} using freedom-before-commitment. */
@SupportedOptions({"NullnessLite"})
public class NullnessChecker extends AbstractNullnessChecker {

    private final String NULLNESS_LITE_STUB = "nullness_lite.astub";

    public NullnessChecker() {
        super(true);
    }

    /**
     * NullnessLite option for Nullness Checker 1. [Initialization Checker] disabled as
     * command-lineoption. 2. [Invalidation of Dataflow] impure methods disallowed as command-line
     * option. 3. [Invalidation of Dataflow] aliasing disallowed, see method canAlias in
     * NullnessStore.java. 4. [KeyFor Checker] assume all keys exist in the map, all Map.get(key)
     * returns nonnull. 5. [Boxing of primitives] all BoxedClass.valueOf(primitiveType) are pure,
     * returned Object are equal by ==.
     */
    public void initChecker() {

        if (this.hasOption("NullnessLite")) {
            Map<String, String> nullness_lite = new HashMap<String, String>();
            nullness_lite.put("suppressWarnings", "uninitialized"); // for 1
            nullness_lite.put("assumeSideEffectFree", null); // for 2
            nullness_lite.put("stubs", NULLNESS_LITE_STUB); // for 5
            //            nullness_lite.put("suppressWarnings", "keyfor");

            this.addOptions(nullness_lite);
        }

        super.initChecker();
    }
}
