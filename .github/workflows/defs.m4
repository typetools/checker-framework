changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line".])dnl
dnl
define([dependsOn], [needs])dnl
dnl
define([job_name], [$1:])dnl
dnl
ifelse([The Gradle distribution is the same in every job, so all jobs share one
cache entry, whose key mentions no job. The key covers only the file that pins
the distribution's version. The distribution cache has no "restore-keys",
because a distribution of the wrong version is useless: Gradle would download
the pinned version anyway, and the stale distribution would bloat the cache.])dnl
ifelse([The module cache is per group of jobs rather than per job. A cache per
job would hold about 20 copies of a 350MB cache, and CodeQL's caches already
account for most of the repository's 10GB cache quota. The 4 groups below
hold 4 copies.])dnl
ifelse(["actions/cache" saves nothing when the key was an exact hit, and only
one job can reserve a key, so whichever job in a group saves first fixes that
entry's contents until the key changes. A job that resolves more than the
entry holds downloads the difference on every run, so a job belongs in a
group with jobs that resolve the same dependencies. A job that is the only
one to resolve a dependency pinned by a file in the key gets its own
group:])dnl
ifelse([ * "nonjunit" is the only group that runs ":checker:exampleTests",
   which builds "docs/examples/errorprone" and "docs/examples/lombok". Those
   builds pin plugin and library versions that this project does not use.])dnl
ifelse([ * "misc" is the only group that runs Spotless, the Javadoc linting
   tasks, and the manual's build.])dnl
ifelse([ * "plume-lib" is the lone job that runs Gradle in another project's
   directory, once per plume-lib package.])dnl
ifelse([Group "cf" holds every other job. Those jobs build and test this
project alone: Daikon builds with "make" and Guava with Maven, whose
artifacts this cache does not cover.])dnl
ifelse([The key must cover every file that pins a dependency version. Add to
that list any file that gains a hardcoded dependency or plugin version. One
key formula serves every group, so a change to a file that only one group
resolves invalidates all of them, which costs one run.])dnl
ifelse([A "restore-keys" entry is a key prefix. The bare "gradle-modules-"
entry lets a group whose own entry does not exist yet start from another
group's cache rather than from nothing. That is a partial hit rather than an
exact one, so the job goes on to save its own entry. Neither module key is a
prefix of a "gradle-wrapper-" key, so neither cache can restore the
other.])dnl
ifelse([A "!" pattern removes files that an earlier pattern matched, so the
include pattern must enumerate files, via "/**", rather than name the
directory, which "actions/cache" would archive whole.])dnl
ifelse([Takes 1 argument: the cache group.])dnl
define([gradle_cache], [dnl
      - uses: actions/cache@v6
        with:
          path: ~/.gradle/wrapper
          key: gradle-wrapper-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties') }}
      - uses: actions/cache@v6
        with:
          path: |
            ~/.gradle/caches/modules-2/**
            !~/.gradle/caches/modules-2/**/*.lock
            !~/.gradle/caches/modules-2/gc.properties
          key: gradle-modules-$1-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties', 'gradle/libs.versions.toml', 'buildSrc/build.gradle', 'docs/examples/errorprone/build.gradle', 'docs/examples/lombok/build.gradle') }}
          restore-keys: |
            gradle-modules-$1-
            gradle-modules-
])dnl
dnl
ifelse([Takes 1 argument: the name of the test script that a job runs.
Expands to the cache group that the job belongs to.])dnl
define([cache_group], [dnl
ifelse($1,test-cftests-nonjunit.sh,[nonjunit],
       $1,test-plume-lib.sh,[plume-lib],
       [cf])])dnl
dnl
ifelse([Gradle derives its user home from the JVM's "user.home" property, which
on Linux comes from the passwd database rather than from "$HOME". Each job
that runs Gradle runs as root in a container, so Gradle would write to
"/root/.gradle", whereas "actions/cache" expands "~" to "$HOME", which the
runner sets to "/github/home". Setting GRADLE_USER_HOME makes the two agree,
so that the caches above hold the files that Gradle wrote. "/github/home"
exists only in a container job, which is why this is per job rather than for
the whole workflow.])dnl
define([gradle_user_home], [dnl
    env:
      GRADLE_USER_HOME: /github/home/.gradle
])dnl
dnl
define([clone_plume_scripts_step], [dnl
      - name: clone_plume_scripts
        run: ./checker/bin-devel/clone-plume-scripts.sh
])dnl
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
gradle_user_home()dnl
    steps:
      - uses: actions/checkout@v7
        with:
          set-safe-directory: true
          fetch-depth: 25
          show-progress: false
          persist-credentials: false
gradle_cache(cache_group($3))dnl
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
gradle_user_home()dnl
    steps:
      - uses: actions/checkout@v7
        with:
          set-safe-directory: true
          # Unlimited history for contributors.tex generation.
          fetch-depth: 0
gradle_cache(misc)dnl
clone_plume_scripts_step()dnl
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
