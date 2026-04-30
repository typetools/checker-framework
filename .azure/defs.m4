changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line".])dnl
dnl
define([junit_job], [dnl
  - job: junit_jdk$1
ifelse($1,canary_version,,[    dependsOn:
      - canary_jobs
      - junit_part1_jdk[]canary_version
      - junit_part2_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 70
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: echo $ORG_GRADLE_PROJECT_jdkTestVersion
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
ifelse(["displayName:" must come after "bash:".])dnl
      - bash: ./checker/bin-devel/test-cftests-junit.sh
        displayName: test-cftests-junit.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])dnl
dnl
define([junit_jobs], [dnl
  - job: junit_part1_jdk$1
ifelse($1,canary_version,,[    dependsOn:
      - canary_jobs
      - junit_part1_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 70
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-junit.sh part1
        displayName: test-cftests-junit.sh part1
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
  - job: junit_part2_jdk$1
ifelse($1,canary_version,,[    dependsOn:
      - canary_jobs
      - junit_part2_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 70
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-junit.sh part2
        displayName: test-cftests-junit.sh part2
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])dnl
dnl
define([nonjunit_job], [dnl
  - job: nonjunit_jdk$1
ifelse($1,canary_version,,[    dependsOn:
      - canary_jobs
      - nonjunit_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-nonjunit.sh
        displayName: test-cftests-nonjunit.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])dnl
dnl
define([inference_job], [dnl
ifelse($1,canary_version,[dnl
  # Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  - job: inference_part1_jdk$1
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 90
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-inference-part1.sh
        displayName: test-cftests-inference-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
  - job: inference_part2_jdk$1
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 90
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-inference-part2.sh
        displayName: test-cftests-inference-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
],[dnl
  - job: inference_jdk$1
    dependsOn:
      - canary_jobs
      - inference_part1_jdk[]canary_version
      - inference_part2_jdk[]canary_version
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 90
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-cftests-inference.sh
        displayName: test-cftests-inference.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
])dnl
])dnl
dnl
define([misc_job], [dnl
  - job: misc_jdk$1
ifelse($1,canary_version,,$1,latest_version,,[    dependsOn:
      - canary_jobs
      - misc_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1-plus[]docker_testing:latest
    steps:
      - checkout: self
        # Unlimited fetchDepth (0) for misc jobs, because of need to make contributors.tex.
        fetchDepth: 0
      - bash: ./checker/bin-devel/test-misc.sh
        displayName: test-misc.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_version,[dnl
  - job: typecheck_part1_jdk$1
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - checkout: self
        fetchDepth: 0
      - bash: ./checker/bin-devel/test-typecheck-part1.sh
        displayName: test-typecheck-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
  - job: typecheck_part2_jdk$1
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - checkout: self
        fetchDepth: 0
      - bash: ./checker/bin-devel/test-typecheck-part2.sh
        displayName: test-typecheck-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1], [dnl
  - job: typecheck_jdk$1
    dependsOn:
      - canary_jobs
      - typecheck_part1_jdk[]canary_version
      - typecheck_part2_jdk[]canary_version
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - checkout: self
        fetchDepth: 0
      - bash: ./checker/bin-devel/test-typecheck.sh
        displayName: test-typecheck.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])])dnl
dnl
define([daikon_job], [dnl
  - job: daikon_part1_jdk$1
    dependsOn:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - daikon_part1_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 70
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-daikon-part1.sh
        displayName: test-daikon-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
  - job: daikon_part2_jdk$1
    dependsOn:
      - canary_jobs
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 80
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-daikon-part2.sh
        displayName: test-daikon-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1
  - job: daikon_part3_jdk$1
    dependsOn:
      - canary_jobs
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 80
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-daikon-part3.sh
        displayName: test-daikon-part3.sh]
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1)dnl
dnl
define([guava_job], [dnl
  - job: guava_jdk$1
    dependsOn:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - guava_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeoutInMinutes: 70
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-guava.sh
        displayName: test-guava.sh]
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1)dnl
dnl
define([plume_lib_job], [dnl
  - job: plume_lib_jdk$1
    dependsOn:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - plume_lib_jdk[]canary_version
])dnl
    pool:
      vmImage: 'ubuntu-latest'
    container: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - checkout: self
        fetchDepth: 25
      - bash: ./checker/bin-devel/test-plume-lib.sh
        displayName: test-plume-lib.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: $1])dnl
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
