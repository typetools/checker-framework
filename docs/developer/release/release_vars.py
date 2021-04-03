#!/usr/bin/env python3
# encoding: utf-8
"""
release_vars.py

Created by Jonathan Burke on 2013-02-05.

Copyright (c) 2014 University of Washington. All rights reserved.
"""

# See release_development.html for an explanation of how the release process works
# it will be invaluable when trying to understand the scripts that drive the
# release process

import os
import pwd
import subprocess
import shlex


# ---------------------------------------------------------------------------------
# The only methods that should go here are methods that help define global release
# variables.  All other methods that aid in release should go in release_utils.py


def getAndAppend(name, append):
    """Retrieves the given environment variable and appends the given string to
    its value and returns the new value. The environment variable is not
    modified. Returns an empty string if the environment variable does not
    exist."""
    if name in os.environ:
        return os.environ[name] + append

    else:
        return ""


def execute(command_args, halt_if_fail=True, capture_output=False, working_dir=None):
    """Execute the given command.
    If capture_output is true, then return the output (and ignore the halt_if_fail argument).
    If capture_output is not true, return the return code of the subprocess call."""

    if working_dir is not None:
        print("Executing in %s: %s" % (working_dir, command_args))
    else:
        print("Executing: %s" % (command_args))
    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    if capture_output:
        process = subprocess.Popen(args, stdout=subprocess.PIPE, cwd=working_dir)
        out = process.communicate()[0]
        process.wait()
        return out

    else:
        result = subprocess.call(args, cwd=working_dir)
        if halt_if_fail and result:
            raise Exception("Error %s while executing %s" % (result, args))
        return result


# ---------------------------------------------------------------------------------

# The location the test site is built in
DEV_SITE_URL = "https://checkerframework.org/dev"
DEV_SITE_DIR = "/cse/www2/types/checker-framework/dev"

# The location the test site is pushed to when it is ready
LIVE_SITE_URL = "https://checkerframework.org"
LIVE_SITE_DIR = "/cse/www2/types/checker-framework"

# Per-user directory for the temporary files created by the release process
# ("USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566.
# Another alternative is: USER = os.getenv('USER').)
TMP_DIR = "/scratch/" + pwd.getpwuid(os.geteuid())[0] + "/jsr308-release"

# Location this and other release scripts are contained in
SCRIPTS_DIR = TMP_DIR + "/checker-framework/docs/developer/release"

# Location in which we will download files to run sanity checks
SANITY_DIR = TMP_DIR + "/sanity"

# The existence of this file indicates that release_build completed.
# It is deleted at the beginning of a release_build run, and at the
# end of a release_push run.
RELEASE_BUILD_COMPLETED_FLAG_FILE = TMP_DIR + "/release-build-completed"

# Every time a release is built the changes/tags are pushed here
INTERM_REPO_ROOT = TMP_DIR + "/interm"
INTERM_CHECKER_REPO = os.path.join(INTERM_REPO_ROOT, "checker-framework")
INTERM_ANNO_REPO = os.path.join(INTERM_REPO_ROOT, "annotation-tools")

# The central repositories for Checker Framework related projects
LIVE_ANNO_REPO = "git@github.com:typetools/annotation-tools.git"
LIVE_CHECKER_REPO = "git@github.com:typetools/checker-framework.git"
PLUME_SCRIPTS_REPO = "https://github.com/plume-lib/plume-scripts"
CHECKLINK_REPO = "https://github.com/plume-lib/checklink"
PLUME_BIB_REPO = "https://github.com/mernst/plume-bib"
STUBPARSER_REPO = "https://github.com/typetools/stubparser"

# Location of the project directories in which we will build the actual projects.
# When we build these projects are pushed to the INTERM repositories.
BUILD_DIR = TMP_DIR + "/build/"
CHECKER_FRAMEWORK = os.path.join(BUILD_DIR, "checker-framework")
CHECKER_FRAMEWORK_RELEASE = os.path.join(CHECKER_FRAMEWORK, "docs/developer/release")

# If a new Gradle wrapper was recently installed, the first ./gradlew command may output:
#   Downloading https://services.gradle.org/distributions/gradle-6.6.1-bin.zip
# This first call might output Gradle diagnostics, such as "downloading".
execute(
    "./gradlew version -q", True, True, TMP_DIR + "/checker-framework"
)
CF_VERSION = (
    execute("./gradlew version -q", True, True, TMP_DIR + "/checker-framework")
    .strip()
    .decode("utf-8")
)

ANNO_TOOLS = os.path.join(BUILD_DIR, "annotation-tools")
ANNO_FILE_UTILITIES = os.path.join(ANNO_TOOLS, "annotation-file-utilities")

PLUME_SCRIPTS = os.path.join(BUILD_DIR, "plume-scripts")
CHECKLINK = os.path.join(BUILD_DIR, "checklink")
PLUME_BIB = os.path.join(BUILD_DIR, "plume-bib")
STUBPARSER = os.path.join(BUILD_DIR, "stubparser")

BUILD_REPOS = (CHECKER_FRAMEWORK, ANNO_TOOLS)
INTERM_REPOS = (INTERM_CHECKER_REPO, INTERM_ANNO_REPO)

INTERM_TO_BUILD_REPOS = (
    (INTERM_CHECKER_REPO, CHECKER_FRAMEWORK),
    (INTERM_ANNO_REPO, ANNO_TOOLS),
)

LIVE_TO_INTERM_REPOS = (
    (LIVE_CHECKER_REPO, INTERM_CHECKER_REPO),
    (LIVE_ANNO_REPO, INTERM_ANNO_REPO),
)

AFU_LIVE_SITE = os.path.join(LIVE_SITE_DIR, "annotation-file-utilities")
AFU_LIVE_RELEASES_DIR = os.path.join(AFU_LIVE_SITE, "releases")

CHECKER_LIVE_RELEASES_DIR = os.path.join(LIVE_SITE_DIR, "releases")

os.environ["PARENT_DIR"] = BUILD_DIR
os.environ["CHECKERFRAMEWORK"] = CHECKER_FRAMEWORK
perl_libs = TMP_DIR + "/homes/gws/mernst/bin/src/perl:/usr/share/perl5/"
# Environment variables for tools needed during the build
os.environ["PLUME_SCRIPTS"] = PLUME_SCRIPTS
os.environ["CHECKLINK"] = CHECKLINK
os.environ["BIBINPUTS"] = ".:" + PLUME_BIB
os.environ["TEXINPUTS"] = ".:/homes/gws/mernst/tex/sty:/homes/gws/mernst/tex:..:"
os.environ["PERLLIB"] = getAndAppend("PERLLIB", ":") + perl_libs
os.environ["PERL5LIB"] = getAndAppend("PERL5LIB", ":") + perl_libs
# Still needed for santiy checks
os.environ["JAVA_8_HOME"] = "/usr/lib/jvm/java-1.8.0-openjdk/"
os.environ["JAVA_HOME"] = os.environ["JAVA_8_HOME"]

EDITOR = os.getenv("EDITOR")
if EDITOR is None:
    EDITOR = "emacs"

PATH = os.environ["JAVA_HOME"] + "/bin:" + os.environ["PATH"]
PATH = PATH + ":/usr/bin"
PATH = PATH + ":" + PLUME_SCRIPTS
PATH = PATH + ":" + CHECKLINK
PATH = PATH + ":/homes/gws/mernst/.local/bin"  # for html5validator
PATH = PATH + ":."
os.environ["PATH"] = PATH

# Tools that must be on your PATH (besides common Unix ones like grep)
TOOLS = ["hevea", "perl", "java", "latex", "mvn", "hg", "git", "html5validator", EDITOR]
