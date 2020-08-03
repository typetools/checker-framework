These are test cases for when we disable the framework supports for AutoValue and Lombok, therefore
the checker annotation won't be automatically inserted into the code.
These two test cases are adapted from _autovalue/Animal.java_ and _lombok/LombokBuilderExample.java_
but removed all `// :: error:` comments.

Parcel and Parcelable have to be included here to avoid a crash in the auto-value-parcel plugin.
