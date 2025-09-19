# DO NOT EDIT azure-pipelines.yml.  Edit azure-pipelines.yml.m4 and defs.m4 instead.

changequote(`[',`]')dnl
include([defs.m4])dnl
# Workaround for https://status.dev.azure.com/_event/179641421
trigger:
  branches:
    include:
    - '*'
pr:
  branches:
    include:
    - '*'

variables:
  system.debug: true

jobs:

# The dependsOn clauses are:
#  * Everything depends on the canary jobs (the main jdk25 jobs), except those jobs themselves.
#  * Any other *_jdkNN job depends on the corresponding *_jdk25 job.

- job: canary_jobs
  dependsOn:
   - junit_jdk[]canary_version
   - nonjunit_jdk[]canary_version
   - inference_part1_jdk[]canary_version
   - inference_part2_jdk[]canary_version
   - typecheck_part1_jdk[]canary_version
   - typecheck_part2_jdk[]canary_version
   - misc_jdk[]canary_version
   - misc_jdk[]latest_version
  pool:
    vmImage: 'ubuntu-latest'
  steps:
  - bash: true
    displayName: canary_jobs

include([jobs.m4])dnl

ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])
