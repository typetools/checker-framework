
# Checker-Framework Issue Fix

## Motivation

Contributing to an open-source project is important and challenging, especially when the codebase already contains many features. Adding a new capability requires a careful study of existing logic. A productive way to contribute is to address issues in a GitHub repository, because those represent real needs from the users of the library or package.

## Your Task

Address the issue [#3211 in the Checker Framework](https://github.com/typetools/checker-framework/issues/3211).

1. **Fork the repository** into your local GitHub account.
2. **Create a new branch** named `issue-3221` from the `main` branch.
3. **Learn the basics** of the Checker Framework using the [official manual](https://checkerframework.org/manual/). (You only need enough understanding to address the issue.)
4. **Make the necessary code changes** on the `issue-3221` branch to resolve the issue.
5. **Enable the provided (currently disabled) test case** [here](https://github.com/typetools/checker-framework/blob/master/checker/tests/signedness/BitPatternOperations.java) for the issue and run it against your changes. Ensure the test passes based on your understanding of the issue.
6. **Under the repository root**, create a new directory named `write-ups/`. Add two files:
   - `successes.txt` describing what worked well when using LLM support
   - `failures.txt` describing what did not

## Grading Process

We will do the following:

1. Clone your fork of the Checker Framework.
2. Checkout the `issue-3221` branch.
3. Compile and run your code on our own test cases to validate the results.
4. Review `write-ups/successes.txt` and `write-ups/failures.txt`.

## Submission

Share a Git URL that points to your fork's `issue-3221` branch so we can clone and run the tests (for example,
`https://github.com/your-username/checker-framework/tree/issue-3221`).

**Good luck and have fun!**
