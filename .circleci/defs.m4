changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line".])dnl
dnl
define([job_name], [$1:])
dnl
ifelse([Takes 4 arguments: OS, JDK version number, name, command line.])dnl
define([boilerplate], [dnl
    docker:
      - image: 'mdernst/cf-$1-jdk$2[]docker_testing'
    resource_class: large
    environment:
      TERM: dumb
    steps:
      - restore_cache:
          keys:
            - &source-cache source-v1-{{ .Branch }}-{{ .Revision }}
            - 'source-v1-{{ .Branch }}-'
            - source-v1-
      - checkout[]ifelse(,full,[:
          method: full])
      - save_cache:
          key: *source-cache
          paths:
            - .git
      - run:
          name: $3
          command: $4
ifelse($3,test-cftests-nonjunit.sh,[],
       $3,test-cftests-inference-part1.sh,[],
       $3,test-cftests-inference-part2.sh,[],
       $3,test-plume-lib.sh,[],
       $3,test-typecheck-part1.sh,[],
       $3,test-typecheck-part2.sh,[],
       $3,test-guava-part1.sh,[dnl
          no_output_timeout: "50m"
],
       $3,test-guava-part2.sh,[dnl
          no_output_timeout: "50m"
],
       [dnl
          no_output_timeout: "30m"
])dnl
          environment:
            ORG_GRADLE_PROJECT_jdkTestVersion: $2
])dnl
dnl
ifelse([This macro takes 1-3 arguments: the JDK version and optionally a docker
image name suffix like "-plus", and a checkout method "full".])dnl
define([circleci_boilerplate], [dnl
    docker:
      - image: 'mdernst/cf-ubuntu-jdk$1[]$2[]docker_testing'
    resource_class: large
    environment:
      TERM: dumb
    steps:
      - restore_cache:
          keys:
            - &source$3-cache source-v1$3-{{ .Branch }}-{{ .Revision }}
            - 'source-v1$3-{{ .Branch }}-'
            - source-v1$3-
      - checkout[]ifelse($3,full,[:
          method: full])
      - save_cache:
          key: *source$3-cache
          paths:
            - .git])dnl
dnl
ifelse([Each macro takes one argument, the JDK version.])dnl
dnl
define([junit_job], [dnl
  job_name(junit_jdk$1)
boilerplate(ubuntu, $1, test-cftests-junit.sh, ./checker/bin-devel/test-cftests-junit.sh)dnl
])dnl
dnl
define([junit_jobs], [dnl
  job_name(junit_part1_jdk$1)
boilerplate(ubuntu, $1, test-cftests-junit.sh part1, ./checker/bin-devel/test-cftests-junit.sh part1)dnl
  job_name(junit_part2_jdk$1)
boilerplate(ubuntu, $1, test-cftests-junit.sh part2, ./checker/bin-devel/test-cftests-junit.sh part2)dnl
])dnl
dnl
define([nonjunit_job], [dnl
  job_name(nonjunit_jdk$1)
boilerplate(ubuntu, $1, test-cftests-nonjunit.sh, ./checker/bin-devel/test-cftests-nonjunit.sh)dnl
])dnl
dnl
define([inference_job], [dnl
ifelse($1,canary_jdk, [dnl
  # Split into part1 and part2 only for the inference job that "canary_jobs" depends on.
  job_name(inference_part1_jdk$1)
boilerplate(ubuntu, $1, test-cftests-inference-part1.sh, ./checker/bin-devel/test-cftests-inference-part1.sh)dnl
  job_name(inference_part2_jdk$1)
boilerplate(ubuntu, $1, test-cftests-inference-part2.sh, ./checker/bin-devel/test-cftests-inference-part2.sh)dnl
], [dnl
  job_name(inference_jdk$1)
boilerplate(ubuntu, $1, test-cftests-inference.sh, ./checker/bin-devel/test-cftests-inference.sh)dnl
])dnl
])dnl
dnl
define([misc_job], [dnl
  job_name(misc_jdk$1)
circleci_boilerplate($1,-plus,full)
      - run:
          name: getPlumeScripts
          command: ./gradlew -q getPlumeScripts
      - run:
          name: test-misc.sh
          command: ./checker/bin-devel/test-misc.sh
          environment:
            ORG_GRADLE_PROJECT_jdkTestVersion: $1
])dnl
dnl
define([typecheck_job], [dnl
ifelse($1,canary_jdk,[dnl
  job_name(typecheck_part1_jdk$1)
boilerplate(ubuntu, $1, test-typecheck-part1.sh, ./checker/bin-devel/test-typecheck-part1.sh)dnl
  job_name(typecheck_part2_jdk$1)
boilerplate(ubuntu, $1, test-typecheck-part2.sh, ./checker/bin-devel/test-typecheck-part2.sh)dnl
], [dnl
  job_name(typecheck_jdk$1)
boilerplate(ubuntu, $1, test-typecheck.sh, ./checker/bin-devel/test-typecheck.sh)dnl
])])dnl
dnl
define([daikon_job], [dnl
  job_name(daikon_part1_jdk$1)
boilerplate(ubuntu, $1, test-daikon-part1.sh, ./checker/bin-devel/test-daikon-part1.sh)dnl
  job_name(daikon_part2_jdk$1)
boilerplate(ubuntu, $1, test-daikon-part2.sh, ./checker/bin-devel/test-daikon-part2.sh)dnl
  job_name(daikon_part3_jdk$1)
boilerplate(ubuntu, $1, test-daikon-part3.sh, ./checker/bin-devel/test-daikon-part3.sh)dnl
])dnl
dnl
define([guava_job], [dnl
  job_name(guava_part1_jdk$1)
boilerplate(ubuntu, $1, test-guava-part1.sh, ./checker/bin-devel/test-guava-part1.sh)dnl
  job_name(guava_part2_jdk$1)
boilerplate(ubuntu, $1, test-guava-part2.sh, ./checker/bin-devel/test-guava-part2.sh)dnl
])dnl
dnl
define([plume_lib_job], [dnl
  job_name(plume_lib_jdk$1)
boilerplate(ubuntu, $1, test-plume-lib.sh, ./checker/bin-devel/test-plume-lib.sh)dnl
])dnl
dnl
define([job_dependences], [dnl
ifelse([This is tricky because whether the ":" should appear depends on whether the subsequent "requires: exists,])dnl
      - $2[]_jdk$1[]dnl
ifelse($2$1,misc[]latest_jdk,,[dnl
ifelse($1,canary_jdk,,[:
          requires:
            - canary_jobs
ifelse($2,junit,[dnl
            - $2_part1_jdk[]canary_jdk
            - $2_part2_jdk[]canary_jdk
],[dnl
            - $2_jdk[]canary_jdk
])dnl
])dnl
])dnl
])dnl
dnl
define([job_dependences_not_in_canary], [dnl
      - $2[]_jdk$1[]:
          requires:
            - canary_jobs
])dnl
dnl
ifelse([
Local Variables:
eval: (add-hook 'after-save-hook '(lambda () (run-command nil "make")) nil 'local)
end:
])dnl
