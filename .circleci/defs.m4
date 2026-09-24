changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line".])dnl
dnl
define([job_name], [$1:])
dnl
ifelse([CircleCI's "checksum" template accepts a single file, so concatenate
every file that pins a dependency version. Add to this list any file that
gains a hardcoded dependency or plugin version.])dnl
define([gradle_restore_cache], [dnl
      - run:
          name: Compute Gradle cache key
          command: >-
            cat gradle/wrapper/gradle-wrapper.properties gradle/libs.versions.toml
            buildSrc/build.gradle docs/examples/errorprone/build.gradle
            docs/examples/lombok/build.gradle > /tmp/gradle-cache-key
      - restore_cache:
          keys:
            - &gradle-cache 'gradle-v1-{{ .Environment.CIRCLE_JOB }}-{{ checksum "/tmp/gradle-cache-key" }}'
            - 'gradle-v1-{{ .Environment.CIRCLE_JOB }}-'
            - gradle-v1-])dnl
dnl
define([gradle_save_cache], [dnl
      - run:
          name: Prune Gradle cache
          command: |
            find ~/.gradle/caches -name '*.lock' -delete || true
            rm -f ~/.gradle/caches/modules-2/gc.properties
      - save_cache:
          key: *gradle-cache
          paths:
            - ~/.gradle/caches/modules-2
            - ~/.gradle/wrapper])dnl
dnl
define([clone_plume_scripts_step], [dnl
      - run:
          name: clone_plume_scripts
          command: ./checker/bin-devel/clone-plume-scripts.sh])dnl
dnl
ifelse([CircleCI's "Auto-cancel redundant workflows" setting never cancels a
workflow on the default branch. This step makes a job do no work if its commit
is no longer the tip of its branch, because a later push will test the branch.
If the project defines the CIRCLE_TOKEN environment variable (a CircleCI API
token), the step cancels the workflow, so the untested commit shows as
"canceled" rather than as passing. Otherwise, or if cancellation fails,
"circleci-agent step halt" ends the job successfully, and the commit's status
is green although it was not tested. The step does nothing if "git ls-remote"
stalls (transfers under 1000 bytes/second for 30 seconds) or otherwise
fails, or finds no such branch, as for a pull request from a fork, whose
CIRCLE_BRANCH is "pull/NNNN".])dnl
define([halt_if_superseded_step], [dnl
      - run:
          name: halt-if-superseded
          command: |
            if test -n "${CIRCLE_BRANCH:-}"; then
              tip=$(git -c http.lowSpeedLimit=1000 -c http.lowSpeedTime=30 ls-remote "https://github.com/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}.git" "refs/heads/${CIRCLE_BRANCH}" | cut -f1) || true
              if test -n "$tip" && test "$tip" != "$CIRCLE_SHA1"; then
                echo "Superseded: ${CIRCLE_BRANCH} is now at ${tip}, not ${CIRCLE_SHA1}."
                if test -n "${CIRCLE_TOKEN:-}" \
                    && wget -q -O /dev/null --post-data= --header="Circle-Token: ${CIRCLE_TOKEN}" \
                      "https://circleci.com/api/v2/workflow/${CIRCLE_WORKFLOW_ID}/cancel"; then
                  echo "Canceled workflow ${CIRCLE_WORKFLOW_ID}; waiting for the cancellation to stop this job."
                  sleep 120
                fi
                echo "Halting without testing ${CIRCLE_SHA1}; its green status does not mean it passed."
                circleci-agent step halt
              fi
            fi])dnl
dnl
ifelse([Takes 4 arguments: OS, JDK version number, name, command line.])dnl
define([boilerplate], [dnl
    docker:
      - image: 'mdernst/cf-$1-jdk$2[]docker_testing'
    resource_class: large
    environment:
      TERM: dumb
    steps:
halt_if_superseded_step()
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
gradle_restore_cache()
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
gradle_save_cache()
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
halt_if_superseded_step()
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
            - .git
gradle_restore_cache()])dnl
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
clone_plume_scripts_step()
      - run:
          name: test-misc.sh
          command: ./checker/bin-devel/test-misc.sh
          environment:
            ORG_GRADLE_PROJECT_jdkTestVersion: $1
gradle_save_cache()
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
