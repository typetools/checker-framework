The file `testin.txt` in this directory contains a list of github repositories that
are used to test the `wpi-many.sh` script (stored in `checker/bin`). Each entry is a
git URL and commit hash, separated by whitespace.

The projects listed in `testin.txt` are derived from plume-lib projects; each is a hard fork.
These forks have had their (inferrable) annotations removed, and their typical checker
build infrastructure disabled. The `./gradlew wpiManyTest` task defined in `checker/build.gradle`
runs the `wpi-many.sh` script on these projects, and then checks that they typecheck afterwards.
The use of a hard fork means these tests may fail to compile under newer versions of the JDK.

To add a new project (named `$PROJECT` below) to `testin.txt`, follow these steps:
1. Create a new GitHub repository under your own user name with the name "wpi-many-tests-$PROJECT".
Do not initialize it with any content.
2. Clone that repo and the project you intend to add locally.
3. Copy the contents of the project you are adding to your new repo and commit the result.
4. Run `java -cp $CHECKERFRAMEWORK/checker/dist/checker.jar org.checkerframework.framework.stub.RemoveAnnotationsForInference .` on your new repository.
5. Edit the build file of the new project and disable the Checker Framework. This is usually done
by commenting out the checkerframework-gradle-plugin and checkerFramework lines for Gradle users.
In addition, you probably will need to add `org.checkerframework:checker-qual:$LATEST-CF-VERSION` as an
`implementation` dependency. Ensure that the build still succeeds (correcting any errors) and then commit.
6. Run `wpi.sh` on the project yourself and locate any annotations that cannot be inferred by WPI and/or
any false positives from the relevant typecheckers (see `checker/build.gradle` for the list of
checkers that the `wpiManyTest` task uses). You must add annotations or warning suppressions to your
project until running `wpi.sh` on the project with those checkers produces no errors in its final iteration.
When that is the case, commit the result and note the commit hash.
7. Push the state of your new repo to GitHub.
8. Add the HTTPS version of the GitHub URL (without the `.git`!) and the commit hash you noted in step 6
to `testin.txt`.
9. Run `./gradlew wpiManyTest` to ensure that the tests still pass, and correct any errors.
