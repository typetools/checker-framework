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
#  * Everything depends on the canary jobs (the main jdk21 jobs), except those jobs themselves.
#  * Anything *_jdk11 or *_jdk17 or *_jdk21 depends on *_jdk24.

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

junit_job(canary_version)

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
misc_job(24)

typecheck_job_split(canary_version)

daikon_job_split(canary_version)

## I'm not sure why the guava_jdk11 job is failing (it's due to Error Prone).
guava_job(canary_version)

plume_lib_job(canary_version)

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
