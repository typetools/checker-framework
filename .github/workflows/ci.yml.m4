# DO NOT EDIT ci.yml. Edit ci.yml.m4 and defs.m4 instead.

changequote(`[',`]')dnl
include([../../.azure/defs-common.m4])dnl
include([defs.m4])dnl
name: CI

on:
  push:
    branches:
      - "**"
  pull_request:
    branches:
      - "**"

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
      - junit_part1_jdk[]canary_version
      - junit_part2_jdk[]canary_version
      - nonjunit_jdk[]canary_version
      - inference_part1_jdk[]canary_version
      - inference_part2_jdk[]canary_version
      - typecheck_part1_jdk[]canary_version
      - typecheck_part2_jdk[]canary_version
      - misc_jdk[]canary_version
      - misc_jdk[]latest_version
    runs-on: ubuntu-latest
    steps:
      - name: canary_jobs
        run: true

include([../../.azure/jobs.m4])dnl

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])
