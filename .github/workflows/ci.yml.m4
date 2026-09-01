# DO NOT EDIT ci.yml. Edit ci.yml.m4 and defs.m4 instead.

changequote(`[',`]')dnl
include([../../.azure/defs-common.m4])dnl
include([defs.m4])dnl
name: CI

"on":
  push:
    branches:
      - "**"
  pull_request:
    branches:
      - "**"

# Auto-cancel any in-progress jobs from the same branch or PR.
concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

env:
  GIT_CONFIG_COUNT: "1"
  GIT_CONFIG_KEY_0: safe.directory
  GIT_CONFIG_VALUE_0: ${{ github.workspace }}

jobs:

  # The needs clauses are:
  #  * Everything depends on the canary jobs (the main jdk25 jobs), except those jobs themselves.
  #  * Any other *_jdkNN job depends on the corresponding *_jdk25 job.

  canary_jobs:
    needs:
      - junit_part1_jdk[]canary_jdk
      - junit_part2_jdk[]canary_jdk
      - nonjunit_jdk[]canary_jdk
      - inference_part1_jdk[]canary_jdk
      - inference_part2_jdk[]canary_jdk
      - typecheck_part1_jdk[]canary_jdk
      - typecheck_part2_jdk[]canary_jdk
      - misc_jdk[]canary_jdk
      - misc_jdk[]latest_jdk
    runs-on: ubuntu-latest
    steps:
      - name: canary_jobs
        run: true
  ci_info:
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk[]canary_jdk-plus:latest
    steps:
      - uses: actions/checkout@v7
        with:
          set-safe-directory: true
          fetch-depth: 0
      - name: clone_plume_scripts
        run: ./gradlew -q getPlumeScripts
      - name: ci_org_and_branch
        run: ./checker/bin-devel/.plume-scripts/ci-org-and-branch --debug
      - name: git_changes
        run: ./checker/bin-devel/.plume-scripts/git-changes --debug

include([../../.azure/jobs.m4])dnl

  all_green:
    if: always()
    needs:
      - junit_jdk17
      - junit_jdk21
      - junit_jdk26
      - nonjunit_jdk21
      - misc_jdk21
      - guava_part1_jdk[]canary_jdk
      - guava_part2_jdk[]canary_jdk
      - plume_lib_jdk[]canary_jdk
    runs-on: ubuntu-latest
    steps:
      - name: Fail if any dependency failed
        if: contains(needs.*.result, 'failure') || contains(needs.*.result, 'cancelled')
        run: exit 1

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
