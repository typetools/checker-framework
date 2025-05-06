# DO NOT EDIT azure-pipelines-daily.yml.  Edit azure-pipelines-daily.yml.m4 and defs.m4 instead.

changequote(`[',`]')dnl
include([defs.m4])dnl
trigger: none
pr: none

schedules:
# 8am UTC is midnight PST.
- cron: '0 8 * * *'
  displayName: Daily midnight build
  branches:
    include:
    - '*'

variables:
  system.debug: true

jobs:

# The dependsOn clauses are:
#  * Everything depends on the canary jobs (the main jdk24 jobs), except those jobs themselves.
#  * Anything *_jdk11 or *_jdk17 or *_jdk21 depends on *_jdk24.

- job: canary_jobs
  dependsOn:
   - junit_jdk[]canary_version
   - nonjunit_jdk[]canary_version
   - inference_part1_jdk[]canary_version
   - inference_part2_jdk[]canary_version
   - typecheck_part1_jdk[]canary_version
   - typecheck_part2_jdk[]canary_version
  pool:
    vmImage: 'ubuntu-latest'
  steps:
  - bash: true
    displayName: canary_jobs

junit_job(11)
junit_job(17)
junit_job(21)
junit_job(24)

nonjunit_job(11)
nonjunit_job(17)
nonjunit_job(21)
nonjunit_job(24)

# Sometimes one of the invocations of wpi-many in `./gradlew wpiManyTest`
# takes much longer to complete than normal, and this Azure job times out.
# When there is a timeout, one cannot examine wpi or wpi-many logs.
# So use a timeout of 90 minutes, and hope that is enough.
inference_job(11)
inference_job(17)
inference_job(21)
inference_job_split(24)

# Do not run misc_job daily, because it does diffs that assume it is running in
# a pull request.

typecheck_job(11)
typecheck_job(17)
typecheck_job(21)
typecheck_job_split(24)

daikon_job(11)
daikon_job(17)
daikon_job(21)
daikon_job_split(24)

## I think the guava_jdk11 job is failing due to Error Prone not supporting JDK 11.
guava_job(17)
guava_job(21)
guava_job(24)

plume_lib_job(11)
plume_lib_job(17)
plume_lib_job(21)
plume_lib_job(24)

## The downstream jobs are not currently needed because test-downstream.sh is empty.
# - job: downstream_jdk11
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk11:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk17
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk17:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk21
#   dependsOn:
#    - canary_jobs
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk21:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
# - job: downstream_jdk24
#   dependsOn:
#    - canary_jobs
#    - downstream_jdk21
#   pool:
#     vmImage: 'ubuntu-latest'
#   container: mdernst/cf-ubuntu-jdk24:latest
#   steps:
#   - checkout: self
#     fetchDepth: 25
#   - bash: ./checker/bin-devel/test-downstream.sh
#     displayName: test-downstream.sh
dnl
ifelse([
Local Variables:
eval: (make-local-variable 'after-save-hook)
eval: (add-hook 'after-save-hook '(lambda () (compile "make")))
end:
])
