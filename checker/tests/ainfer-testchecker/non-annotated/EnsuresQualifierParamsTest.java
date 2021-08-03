import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.Parent;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;
import org.checkerframework.framework.qual.EnsuresQualifier;

class EnsuresQualifierParamsTest {

    // these methods are used to infer types

    @SuppressWarnings("contracts.postcondition") // establish ground truth
    @EnsuresQualifier(expression = "#1", qualifier = Parent.class)
    void becomeParent(Object arg) {}

    @SuppressWarnings("contracts.postcondition") // establish ground truth
    @EnsuresQualifier(expression = "#1", qualifier = Sibling1.class)
    void becomeSibling1(Object arg) {}

    @SuppressWarnings("contracts.postcondition") // establish ground truth
    @EnsuresQualifier(expression = "#1", qualifier = Sibling2.class)
    void becomeSibling2(Object arg) {}

    @SuppressWarnings("contracts.postcondition") // establish ground truth
    @EnsuresQualifier(expression = "#1", qualifier = AinferBottom.class)
    void becomeBottom(Object arg) {}

    // these methods should have types inferred for them

    void argIsParent(Object arg) {
        becomeParent(arg);
    }

    void argIsParent_2(Object arg, boolean b) {
        if (b) {
            becomeSibling1(arg);
        } else {
            becomeSibling2(arg);
        }
    }

    void argIsSibling2(Object arg) {
        becomeSibling2(arg);
    }

    void argIsSibling2_2(Object arg, boolean b) {
        if (b) {
            becomeSibling2(arg);
        } else {
            becomeBottom(arg);
        }
    }

    void thisIsParent() {
        becomeParent(this);
    }

    void thisIsParent_2(boolean b) {
        if (b) {
            becomeSibling1(this);
        } else {
            becomeSibling2(this);
        }
    }

    void thisIsParent_2_2(boolean b) {
        if (b) {
            becomeSibling2(this);
        } else {
            becomeSibling1(this);
        }
    }

    void thisIsParent_3(boolean b) {
        if (b) {
            becomeSibling1(this);
        } else {
            becomeSibling2(this);
        }
        noEnsures();
    }

    void thisIsEmpty(boolean b) {
        if (b) {
            // do nothing
            this.noEnsures();
        } else {
            becomeSibling1(this);
        }
    }

    void thisIsSibling2() {
        becomeSibling2(this);
    }

    void thisIsSibling2_2(boolean b) {
        if (b) {
            becomeSibling2(this);
        } else {
            becomeBottom(this);
        }
    }

    void thisIsSibling2_2_2(boolean b) {
        if (b) {
            becomeBottom(this);
        } else {
            becomeSibling2(this);
        }
    }

    void noEnsures() {}

    void client1(Object arg) {
        argIsParent(arg);
        // :: warning: (assignment.type.incompatible)
        @Parent Object p = arg;
    }

    void client2(Object arg) {
        argIsParent_2(arg, true);
        // :: warning: (assignment.type.incompatible)
        @Parent Object p = arg;
    }

    void client3(Object arg) {
        argIsSibling2(arg);
        // :: warning: (assignment.type.incompatible)
        @Sibling2 Object x = arg;
    }

    void client4(Object arg) {
        argIsSibling2_2(arg, true);
        // :: warning: (assignment.type.incompatible)
        @Sibling2 Object x = arg;
    }

    void clientThis1() {
        thisIsParent();
        // :: warning: (assignment.type.incompatible)
        @Parent Object o = this;
    }

    void clientThis2() {
        thisIsParent_2(true);
        // :: warning: (assignment.type.incompatible)
        @Parent Object o = this;
    }

    void clientThis2_2() {
        thisIsParent_2(false);
        // :: warning: (assignment.type.incompatible)
        @Parent Object o = this;
    }

    void clientThis2_3() {
        thisIsParent_3(false);
        // :: warning: (assignment.type.incompatible)
        @Parent Object o = this;
    }

    void clientThis3() {
        thisIsSibling2();
        // :: warning: (assignment.type.incompatible)
        @Sibling2 Object o = this;
    }

    void clientThis4() {
        thisIsSibling2_2(true);
        // :: warning: (assignment.type.incompatible)
        @Sibling2 Object o = this;
    }

    void clientThis5() {
        thisIsSibling2_2_2(true);
        // :: warning: (assignment.type.incompatible)
        @Sibling2 Object o = this;
    }

    void clientThis6() {
        thisIsParent_2_2(true);
        // :: warning: (assignment.type.incompatible)
        @Parent Object o = this;
    }
}
