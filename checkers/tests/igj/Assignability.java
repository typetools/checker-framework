import checkers.igj.quals.*;

@I
public class Assignability {
    int field = 3;
    @Assignable int assignable;

    public Assignability() {
        field = 1;
        field *= 1;
        field /= 1;
        field += 1;
        field -= 1;
        field++;
        ++field;
        field--;
        --field;

        this.field = 1;
        this.field *= 1;
        this.field /= 1;
        this.field += 1;
        this.field -= 1;
        this.field++;
        ++this.field;
        this.field--;
        --this.field;

        assignable = 1;
        assignable *= 1;
        assignable /= 1;
        assignable += 1;
        assignable -= 1;
        assignable++;
        ++assignable;
        assignable--;
        --assignable;

        this.assignable = 1;
        this.assignable *= 1;
        this.assignable /= 1;
        this.assignable += 1;
        this.assignable -= 1;
        this.assignable++;
        ++this.assignable;
        this.assignable--;
        --this.assignable;

        @Mutable Assignability mutableRef = null;
        mutableRef.field = 1;
        mutableRef.field *= 1;
        mutableRef.field /= 1;
        mutableRef.field += 1;
        mutableRef.field -= 1;
        mutableRef.field++;
        ++mutableRef.field;
        mutableRef.field--;
        --mutableRef.field;
        mutableRef.assignable = 1;
        mutableRef.assignable *= 1;
        mutableRef.assignable /= 1;
        mutableRef.assignable += 1;
        mutableRef.assignable -= 1;
        mutableRef.assignable++;
        ++mutableRef.assignable;
        mutableRef.assignable--;
        --mutableRef.assignable;

        @ReadOnly Assignability readOnlyRef = (@ReadOnly Assignability)null;
        readOnlyRef.field = 1; // error
        readOnlyRef.field *= 1;    // error
        readOnlyRef.field /= 1;    // error
        readOnlyRef.field += 1;    // error
        readOnlyRef.field -= 1;    // error
        readOnlyRef.field++;   // error
        ++readOnlyRef.field;   // error
        readOnlyRef.field--;   // error
        --readOnlyRef.field;   // error
        readOnlyRef.assignable = 1;
        readOnlyRef.assignable *= 1;
        readOnlyRef.assignable /= 1;
        readOnlyRef.assignable += 1;
        readOnlyRef.assignable -= 1;
        readOnlyRef.assignable++;
        ++readOnlyRef.assignable;
        readOnlyRef.assignable--;
        --readOnlyRef.assignable;

        @Immutable Assignability immutableRef = (@Immutable Assignability)null;
        immutableRef.field = 1; // error
        immutableRef.field *= 1;    // error
        immutableRef.field /= 1;    // error
        immutableRef.field += 1;    // error
        immutableRef.field -= 1;    // error
        immutableRef.field++;   // error
        ++immutableRef.field;   // error
        immutableRef.field--;   // error
        --immutableRef.field;   // error
        immutableRef.assignable = 1;
        immutableRef.assignable *= 1;
        immutableRef.assignable /= 1;
        immutableRef.assignable += 1;
        immutableRef.assignable -= 1;
        immutableRef.assignable++;
        ++immutableRef.assignable;
        immutableRef.assignable--;
        --immutableRef.assignable;
    }

    public void readOnly(@ReadOnly Assignability this) {
        field = 1;  // error
        field *= 1; // error
        field /= 1; // error
        field += 1; // error
        field -= 1; // error
        field++;    // error
        ++field;    // error
        field--;    // error
        --field;    // error

        this.field = 1; // error
        this.field *= 1;    // error
        this.field /= 1;    // error
        this.field += 1;    // error
        this.field -= 1;    // error
        this.field++;   // error
        ++this.field;   // error
        this.field--;   // error
        --this.field;   // error

        assignable = 1;
        assignable *= 1;
        assignable /= 1;
        assignable += 1;
        assignable -= 1;
        assignable++;
        ++assignable;
        assignable--;
        --assignable;

        this.assignable = 1;
        this.assignable *= 1;
        this.assignable /= 1;
        this.assignable += 1;
        this.assignable -= 1;
        this.assignable++;
        ++this.assignable;
        this.assignable--;
        --this.assignable;

        @Mutable Assignability mutableRef = (@Mutable Assignability)null;
        mutableRef.field = 1;
        mutableRef.field *= 1;
        mutableRef.field /= 1;
        mutableRef.field += 1;
        mutableRef.field -= 1;
        mutableRef.field++;
        ++mutableRef.field;
        mutableRef.field--;
        --mutableRef.field;
        mutableRef.assignable = 1;
        mutableRef.assignable *= 1;
        mutableRef.assignable /= 1;
        mutableRef.assignable += 1;
        mutableRef.assignable -= 1;
        mutableRef.assignable++;
        ++mutableRef.assignable;
        mutableRef.assignable--;
        --mutableRef.assignable;

        @ReadOnly Assignability readOnlyRef = (@ReadOnly Assignability)null;
        readOnlyRef.field = 1; // error
        readOnlyRef.field *= 1;    // error
        readOnlyRef.field /= 1;    // error
        readOnlyRef.field += 1;    // error
        readOnlyRef.field -= 1;    // error
        readOnlyRef.field++;   // error
        ++readOnlyRef.field;   // error
        readOnlyRef.field--;   // error
        --readOnlyRef.field;   // error
        readOnlyRef.assignable = 1;
        readOnlyRef.assignable *= 1;
        readOnlyRef.assignable /= 1;
        readOnlyRef.assignable += 1;
        readOnlyRef.assignable -= 1;
        readOnlyRef.assignable++;
        ++readOnlyRef.assignable;
        readOnlyRef.assignable--;
        --readOnlyRef.assignable;

        @Immutable Assignability immutableRef = (@Immutable Assignability)null;
        immutableRef.field = 1; // error
        immutableRef.field *= 1;    // error
        immutableRef.field /= 1;    // error
        immutableRef.field += 1;    // error
        immutableRef.field -= 1;    // error
        immutableRef.field++;   // error
        ++immutableRef.field;   // error
        immutableRef.field--;   // error
        --immutableRef.field;   // error
        immutableRef.assignable = 1;
        immutableRef.assignable *= 1;
        immutableRef.assignable /= 1;
        immutableRef.assignable += 1;
        immutableRef.assignable -= 1;
        immutableRef.assignable++;
        ++immutableRef.assignable;
        immutableRef.assignable--;
        --immutableRef.assignable;

    }

    public void mutable(@Mutable Assignability this) {
        field = 1;
        field *= 1;
        field /= 1;
        field += 1;
        field -= 1;
        field++;
        ++field;
        field--;
        --field;

        this.field = 1;
        this.field *= 1;
        this.field /= 1;
        this.field += 1;
        this.field -= 1;
        this.field++;
        ++this.field;
        this.field--;
        --this.field;

        assignable = 1;
        assignable *= 1;
        assignable /= 1;
        assignable += 1;
        assignable -= 1;
        assignable++;
        ++assignable;
        assignable--;
        --assignable;

        this.assignable = 1;
        this.assignable *= 1;
        this.assignable /= 1;
        this.assignable += 1;
        this.assignable -= 1;
        this.assignable++;
        ++this.assignable;
        this.assignable--;
        --this.assignable;

        @Mutable Assignability mutableRef = (@Mutable Assignability)null;
        mutableRef.field = 1;
        mutableRef.field *= 1;
        mutableRef.field /= 1;
        mutableRef.field += 1;
        mutableRef.field -= 1;
        mutableRef.field++;
        ++mutableRef.field;
        mutableRef.field--;
        --mutableRef.field;
        mutableRef.assignable = 1;
        mutableRef.assignable *= 1;
        mutableRef.assignable /= 1;
        mutableRef.assignable += 1;
        mutableRef.assignable -= 1;
        mutableRef.assignable++;
        ++mutableRef.assignable;
        mutableRef.assignable--;
        --mutableRef.assignable;

        @ReadOnly Assignability readOnlyRef = (@ReadOnly Assignability )null;
        readOnlyRef.field = 1; // error
        readOnlyRef.field *= 1;    // error
        readOnlyRef.field /= 1;    // error
        readOnlyRef.field += 1;    // error
        readOnlyRef.field -= 1;    // error
        readOnlyRef.field++;   // error
        ++readOnlyRef.field;   // error
        readOnlyRef.field--;   // error
        --readOnlyRef.field;   // error
        readOnlyRef.assignable = 1;
        readOnlyRef.assignable *= 1;
        readOnlyRef.assignable /= 1;
        readOnlyRef.assignable += 1;
        readOnlyRef.assignable -= 1;
        readOnlyRef.assignable++;
        ++readOnlyRef.assignable;
        readOnlyRef.assignable--;
        --readOnlyRef.assignable;

        @Immutable Assignability immutableRef = (@Immutable Assignability)null;
        immutableRef.field = 1; // error
        immutableRef.field *= 1;    // error
        immutableRef.field /= 1;    // error
        immutableRef.field += 1;    // error
        immutableRef.field -= 1;    // error
        immutableRef.field++;   // error
        ++immutableRef.field;   // error
        immutableRef.field--;   // error
        --immutableRef.field;   // error
        immutableRef.assignable = 1;
        immutableRef.assignable *= 1;
        immutableRef.assignable /= 1;
        immutableRef.assignable += 1;
        immutableRef.assignable -= 1;
        immutableRef.assignable++;
        ++immutableRef.assignable;
        immutableRef.assignable--;
        --immutableRef.assignable;


    }

    public void immutable(@Immutable Assignability this) {
        field = 1;  // error
        field *= 1; // error
        field /= 1; // error
        field += 1; // error
        field -= 1; // error
        field++;    // error
        ++field;    // error
        field--;    // error
        --field;    // error

        this.field = 1; // error
        this.field *= 1;    // error
        this.field /= 1;    // error
        this.field += 1;    // error
        this.field -= 1;    // error
        this.field++;   // error
        ++this.field;   // error
        this.field--;   // error
        --this.field;   // error

        assignable = 1;
        assignable *= 1;
        assignable /= 1;
        assignable += 1;
        assignable -= 1;
        assignable++;
        ++assignable;
        assignable--;
        --assignable;

        this.assignable = 1;
        this.assignable *= 1;
        this.assignable /= 1;
        this.assignable += 1;
        this.assignable -= 1;
        this.assignable++;
        ++this.assignable;
        this.assignable--;
        --this.assignable;

        @Mutable Assignability mutableRef = (@Mutable Assignability)null;
        mutableRef.field = 1;
        mutableRef.field *= 1;
        mutableRef.field /= 1;
        mutableRef.field += 1;
        mutableRef.field -= 1;
        mutableRef.field++;
        ++mutableRef.field;
        mutableRef.field--;
        --mutableRef.field;
        mutableRef.assignable = 1;
        mutableRef.assignable *= 1;
        mutableRef.assignable /= 1;
        mutableRef.assignable += 1;
        mutableRef.assignable -= 1;
        mutableRef.assignable++;
        ++mutableRef.assignable;
        mutableRef.assignable--;
        --mutableRef.assignable;

        @ReadOnly Assignability readOnlyRef = (@ReadOnly Assignability)null;
        readOnlyRef.field = 1; // error
        readOnlyRef.field *= 1;    // error
        readOnlyRef.field /= 1;    // error
        readOnlyRef.field += 1;    // error
        readOnlyRef.field -= 1;    // error
        readOnlyRef.field++;   // error
        ++readOnlyRef.field;   // error
        readOnlyRef.field--;   // error
        --readOnlyRef.field;   // error
        readOnlyRef.assignable = 1;
        readOnlyRef.assignable *= 1;
        readOnlyRef.assignable /= 1;
        readOnlyRef.assignable += 1;
        readOnlyRef.assignable -= 1;
        readOnlyRef.assignable++;
        ++readOnlyRef.assignable;
        readOnlyRef.assignable--;
        --readOnlyRef.assignable;

        @Immutable Assignability immutableRef = (@Immutable Assignability)null;
        immutableRef.field = 1; // error
        immutableRef.field *= 1;    // error
        immutableRef.field /= 1;    // error
        immutableRef.field += 1;    // error
        immutableRef.field -= 1;    // error
        immutableRef.field++;   // error
        ++immutableRef.field;   // error
        immutableRef.field--;   // error
        --immutableRef.field;   // error
        immutableRef.assignable = 1;
        immutableRef.assignable *= 1;
        immutableRef.assignable /= 1;
        immutableRef.assignable += 1;
        immutableRef.assignable -= 1;
        immutableRef.assignable++;
        ++immutableRef.assignable;
        immutableRef.assignable--;
        --immutableRef.assignable;

    }

    public void mutableByDefault() {
        field = 1;
        field *= 1;
        field /= 1;
        field += 1;
        field -= 1;
        field++;
        ++field;
        field--;
        --field;

        this.field = 1;
        this.field *= 1;
        this.field /= 1;
        this.field += 1;
        this.field -= 1;
        this.field++;
        ++this.field;
        this.field--;
        --this.field;

        assignable = 1;
        assignable *= 1;
        assignable /= 1;
        assignable += 1;
        assignable -= 1;
        assignable++;
        ++assignable;
        assignable--;
        --assignable;

        this.assignable = 1;
        this.assignable *= 1;
        this.assignable /= 1;
        this.assignable += 1;
        this.assignable -= 1;
        this.assignable++;
        ++this.assignable;
        this.assignable--;
        --this.assignable;

        @Mutable Assignability mutableRef = (@Mutable Assignability)null;
        mutableRef.field = 1;
        mutableRef.field *= 1;
        mutableRef.field /= 1;
        mutableRef.field += 1;
        mutableRef.field -= 1;
        mutableRef.field++;
        ++mutableRef.field;
        mutableRef.field--;
        --mutableRef.field;
        mutableRef.assignable = 1;
        mutableRef.assignable *= 1;
        mutableRef.assignable /= 1;
        mutableRef.assignable += 1;
        mutableRef.assignable -= 1;
        mutableRef.assignable++;
        ++mutableRef.assignable;
        mutableRef.assignable--;
        --mutableRef.assignable;

        @ReadOnly Assignability readOnlyRef = (@ReadOnly Assignability)null;
        readOnlyRef.field = 1; // error
        readOnlyRef.field *= 1;    // error
        readOnlyRef.field /= 1;    // error
        readOnlyRef.field += 1;    // error
        readOnlyRef.field -= 1;    // error
        readOnlyRef.field++;   // error
        ++readOnlyRef.field;   // error
        readOnlyRef.field--;   // error
        --readOnlyRef.field;   // error
        readOnlyRef.assignable = 1;
        readOnlyRef.assignable *= 1;
        readOnlyRef.assignable /= 1;
        readOnlyRef.assignable += 1;
        readOnlyRef.assignable -= 1;
        readOnlyRef.assignable++;
        ++readOnlyRef.assignable;
        readOnlyRef.assignable--;
        --readOnlyRef.assignable;

        @Immutable Assignability immutableRef = (@Immutable Assignability)null;
        immutableRef.field = 1; // error
        immutableRef.field *= 1;    // error
        immutableRef.field /= 1;    // error
        immutableRef.field += 1;    // error
        immutableRef.field -= 1;    // error
        immutableRef.field++;   // error
        ++immutableRef.field;   // error
        immutableRef.field--;   // error
        --immutableRef.field;   // error
        immutableRef.assignable = 1;
        immutableRef.assignable *= 1;
        immutableRef.assignable /= 1;
        immutableRef.assignable += 1;
        immutableRef.assignable -= 1;
        immutableRef.assignable++;
        ++immutableRef.assignable;
        immutableRef.assignable--;
        --immutableRef.assignable;

    }


}
