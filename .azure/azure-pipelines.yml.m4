# DO NOT EDIT azure-pipelines.yml.  Edit azure-pipelines.yml.m4 and defs.m4 instead.
changequote
changequote(`[',`]')dnl
include([defs-common.m4])dnl
include([defs.m4])dnl
trigger:
  batch: true
  branches:
    include:
      - '*'
pr:
  branches:
    include:
      - '*'

variables:
  - name: system.debug
    value: true

jobs:

  # The dependsOn clauses are:
  #  * Everything depends on the canary jobs (the main jdk25 jobs), except those jobs themselves.
  #  * Any *_jdkNN job (NN != 25) depends on the corresponding *_jdk25 job.

  - job: canary_jobs
    dependsOn:
      - junit_part1_jdk[]canary_jdk
      - junit_part2_jdk[]canary_jdk
      - nonjunit_jdk[]canary_jdk
      - inference_part1_jdk[]canary_jdk
      - inference_part2_jdk[]canary_jdk
      - typecheck_part1_jdk[]canary_jdk
      - typecheck_part2_jdk[]canary_jdk
      - misc_jdk[]canary_jdk
      - misc_jdk[]latest_jdk
    pool:
      vmImage: 'ubuntu-latest'
    steps:
      - checkout: none
      - bash: true
        displayName: canary_jobs

include([jobs.m4])dnl

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])
