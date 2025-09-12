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

junit_job(11)
junit_job(17)
junit_job(21)
junit_job(25)

nonjunit_job(canary_version)

# Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
# takes much longer to complete than normal, and this Azure job times out.
# When there is a timeout, one cannot examine wpi or wpi-many logs.
# So use a timeout of 90 minutes, and hope that is enough.
inference_job_split(canary_version)

# Unlimited fetchDepth (0) for misc_jobs, because of need to make contributors.tex .
misc_job(11)
misc_job(17)
misc_job(21)
misc_job(25)

typecheck_job_split(canary_version)

daikon_job_split(canary_version)

## I'm not sure why the guava_jdk11 job is failing (it's due to Error Prone).
guava_job(canary_version)

plume_lib_job(canary_version)

ifelse([
Local Variables:
eval: (make-local-variable 'after-save-hook)
eval: (add-hook 'after-save-hook '(lambda () (compile "make")))
end:
])
