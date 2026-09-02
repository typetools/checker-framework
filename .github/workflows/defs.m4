changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line".])dnl
dnl
define([dependsOn], [needs])dnl
dnl
define([job_name], [$1:])dnl
dnl
ifelse([Takes 4 arguments: OS, JDK version number, name, command line.])dnl
define([boilerplate], [dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-$1-jdk$2[]docker_testing:latest
ifelse($3,test-cftests-nonjunit.sh,[],
       $3,test-typecheck-part1.sh,[],
       $3,test-typecheck-part2.sh,[],
       $3,test-plume-lib.sh,[],
       $3,test-cftests-inference-part1.sh,[    timeout-minutes: 90
],
       $3,test-cftests-inference-part2.sh,[    timeout-minutes: 90
],
[    timeout-minutes: 70
])dnl
    steps:
      - uses: actions/checkout@v7
        with:
          set-safe-directory: true
          fetch-depth: 25
      - name: $3
        run: $4
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$2"
])dnl
dnl
define([junit_job], [dnl
  job_name(junit_jdk$1)
ifelse($1,canary_jdk,,[    dependsOn:
      - canary_jobs
      - junit_part1_jdk[]canary_jdk
      - junit_part2_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-cftests-junit.sh, ./checker/bin-devel/test-cftests-junit.sh)dnl
])dnl
dnl
define([junit_jobs], [dnl
  job_name(junit_part1_jdk$1)
ifelse($1,canary_jdk,,[    dependsOn:
      - canary_jobs
      - junit_part1_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-cftests-junit.sh part1, ./checker/bin-devel/test-cftests-junit.sh part1)dnl
  job_name(junit_part2_jdk$1)
ifelse($1,canary_jdk,,[    dependsOn:
      - canary_jobs
      - junit_part2_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-cftests-junit.sh part2, ./checker/bin-devel/test-cftests-junit.sh part2)dnl
])dnl
dnl
define([nonjunit_job], [dnl
  job_name(nonjunit_jdk$1)
ifelse($1,canary_jdk,,[    dependsOn:
      - canary_jobs
      - nonjunit_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-cftests-nonjunit.sh, ./checker/bin-devel/test-cftests-nonjunit.sh)dnl
])dnl
dnl
define([inference_job], [dnl
ifelse($1,canary_jdk,[dnl
  # Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  job_name(inference_part1_jdk$1)
boilerplate(ubuntu, $1, test-cftests-inference-part1.sh, ./checker/bin-devel/test-cftests-inference-part1.sh)dnl
  job_name(inference_part2_jdk$1)
boilerplate(ubuntu, $1, test-cftests-inference-part2.sh, ./checker/bin-devel/test-cftests-inference-part2.sh)dnl
],[dnl
  job_name(inference_jdk$1)
    dependsOn:
      - canary_jobs
      - inference_part1_jdk[]canary_jdk
      - inference_part2_jdk[]canary_jdk
boilerplate(ubuntu, $1, test-cftests-inference.sh, ./checker/bin-devel/test-cftests-inference.sh)dnl
])dnl
])dnl
dnl
define([misc_job], [dnl
  job_name(misc_jdk$1)
ifelse($1,canary_jdk,,$1,latest_jdk,,[    dependsOn:
      - canary_jobs
      - misc_jdk[]canary_jdk
])dnl
    runs-on: ubuntu-latest
    container:
      image: mdernst/cf-ubuntu-jdk$1-plus[]docker_testing:latest
    steps:
      - uses: actions/checkout@v7
        with:
          set-safe-directory: true
          # Unlimited history for contributors.tex generation.
          fetch-depth: 0
      - name: getPlumeScripts
        run: ./gradlew -q getPlumeScripts
      - name: test-misc.sh
        run: ./checker/bin-devel/test-misc.sh
        env:
          ORG_GRADLE_PROJECT_jdkTestVersion: "$1"])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_jdk,[dnl
  job_name(typecheck_part1_jdk$1)
boilerplate(ubuntu, $1, test-typecheck-part1.sh, ./checker/bin-devel/test-typecheck-part1.sh)dnl
  job_name(typecheck_part2_jdk$1)
boilerplate(ubuntu, $1, test-typecheck-part2.sh, ./checker/bin-devel/test-typecheck-part2.sh)dnl
], [dnl
  job_name(typecheck_jdk$1)
    dependsOn:
      - canary_jobs
      - typecheck_part1_jdk[]canary_jdk
      - typecheck_part2_jdk[]canary_jdk
boilerplate(ubuntu, $1, test-typecheck.sh, ./checker/bin-devel/test-typecheck.sh)dnl
])])dnl
dnl
define([daikon_job], [dnl
  job_name(daikon_part1_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - daikon_part1_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-daikon-part1.sh, ./checker/bin-devel/test-daikon-part1.sh)dnl
  job_name(daikon_part2_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - daikon_part2_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-daikon-part2.sh, ./checker/bin-devel/test-daikon-part2.sh)dnl
  job_name(daikon_part3_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - daikon_part3_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-daikon-part3.sh, ./checker/bin-devel/test-daikon-part3.sh)dnl
])dnl
dnl
define([guava_job], [dnl
  job_name(guava_part1_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - guava_part1_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-guava-part1.sh, ./checker/bin-devel/test-guava-part1.sh)dnl
  job_name(guava_part2_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - guava_part2_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-guava-part2.sh, ./checker/bin-devel/test-guava-part2.sh)dnl
])dnl
dnl
define([plume_lib_job], [dnl
  job_name(plume_lib_jdk$1)
    dependsOn:
      - canary_jobs
ifelse($1,canary_jdk,,[dnl
      - plume_lib_jdk[]canary_jdk
])dnl
boilerplate(ubuntu, $1, test-plume-lib.sh, ./checker/bin-devel/test-plume-lib.sh)dnl
])dnl
dnl
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
