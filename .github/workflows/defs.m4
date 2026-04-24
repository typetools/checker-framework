changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" macro means "discard to next line".])dnl
dnl
define([junit_job], [dnl
  junit_jdk$1:
ifelse($1,canary_version,,[    needs:
      - canary_jobs
      - junit_part1_jdk[]canary_version
      - junit_part2_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 70
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-junit.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-junit.sh])dnl
dnl
define([junit_jobs], [dnl
  junit_part1_jdk$1:
ifelse($1,canary_version,,[    needs:
      - canary_jobs
      - junit_part1_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 70
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-junit.sh part1
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-junit.sh part1
  junit_part2_jdk$1:
ifelse($1,canary_version,,[    needs:
      - canary_jobs
      - junit_part2_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 70
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-junit.sh part2
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-junit.sh part2])dnl
dnl
define([nonjunit_job], [dnl
  nonjunit_jdk$1:
ifelse($1,canary_version,,[    needs:
      - canary_jobs
      - nonjunit_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-nonjunit.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-nonjunit.sh])dnl
dnl
define([inference_job], [dnl
ifelse($1,canary_version,[dnl
  # Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  inference_part1_jdk$1:
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 90
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-inference-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-inference-part1.sh
  inference_part2_jdk$1:
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 90
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-inference-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-inference-part2.sh
],[dnl
  inference_jdk$1:
    needs:
      - canary_jobs
      - inference_part1_jdk[]canary_version
      - inference_part2_jdk[]canary_version
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 90
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-cftests-inference.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-cftests-inference.sh
])dnl
])dnl
dnl
define([misc_job], [dnl
  misc_jdk$1:
ifelse($1,canary_version,,$1,latest_version,,[    needs:
      - canary_jobs
      - misc_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1-plus[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          # Unlimited history for contributors.tex generation.
          fetch-depth: 0
      - name: test-misc.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-misc.sh])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_version,[dnl
  # Split into part1 and part2 only for the typecheck job that "canary_jobs" depends on.
  typecheck_part1_jdk$1:
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 1000
      - name: test-typecheck-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-typecheck-part1.sh
  typecheck_part2_jdk$1:
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 1000
      - name: test-typecheck-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-typecheck-part2.sh
], [dnl
  typecheck_jdk$1:
    needs:
      - canary_jobs
      - typecheck_part1_jdk[]canary_version
      - typecheck_part2_jdk[]canary_version
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 1000
      - name: test-typecheck.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-typecheck.sh
])])dnl
dnl
define([daikon_job], [dnl
  daikon_part1_jdk$1:
    needs:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - daikon_part1_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 70
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-daikon-part1.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-daikon-part1.sh
  daikon_part2_jdk$1:
    needs:
      - canary_jobs
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 80
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-daikon-part2.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-daikon-part2.sh
  daikon_part3_jdk$1:
    needs:
      - canary_jobs
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 80
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-daikon-part3.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-daikon-part3.sh])dnl
dnl
define([guava_job], [dnl
  guava_jdk$1:
    needs:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - guava_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    timeout-minutes: 70
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-guava.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-guava.sh])dnl
dnl
define([plume_lib_job], [dnl
  plume_lib_jdk$1:
    needs:
      - canary_jobs
ifelse($1,canary_version,,[dnl
      - plume_lib_jdk[]canary_version
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1[]docker_testing:latest
    steps:
      - uses: actions/checkout@v4
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: test-plume-lib.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"
        run: ./checker/bin-devel/test-plume-lib.sh])dnl
dnl
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
