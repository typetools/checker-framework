// RecordPureLib is read from bytecode created by NullnessRecordPureBytecodeTest.
class RecordPureFromBytecode {
  int generatedAccessorIsPure(RecordPureLib r) {
    if (r.generated() == null) {
      return 0;
    }
    return r.generated().length();
  }

  int explicitAccessorIsNotPure(RecordPureLib r) {
    if (r.explicit() == null) {
      return 0;
    }
    // :: error: [dereference.of.nullable]
    return r.explicit().length();
  }
}
