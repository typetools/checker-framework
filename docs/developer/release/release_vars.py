#!/usr/bin/env python
"""Release variables."""

# See release_development.html for an explanation of how the release process works
# it will be invaluable when trying to understand the scripts that drive the
# release process

from __future__ import annotations

import os
import pwd
import shlex
import subprocess
from pathlib import Path

# ---------------------------------------------------------------------------------
# The only methods that should go here are methods that help define global release
# variables.  All other methods that aid in release should go in release_utils.py


def get_and_append(name: str, append: str) -> str:
    """Return the given environment variable plus `append`, or an empty string.

    Return an empty string if the environment variable does not exist.

    Returns:
        the given environment variable plus `append`, or an empty string.
    """
    if name in os.environ:
        return os.environ[name] + append

    return ""


def execute_output(
    command: str | list[str],
    working_dir: Path | None = None,
) -> str:
    """Execute the given command.

    Returns:
        the output
    """
    if working_dir is not None:
        print(f"Executing in {working_dir}: {command}")
    else:
        print(f"Executing: {command}")
    command_line = shlex.split(command) if isinstance(command, str) else command

    process = subprocess.Popen(command_line, stdout=subprocess.PIPE, cwd=working_dir)
    out = process.communicate()[0]
    process.wait()
    return out.decode("utf-8")


def execute(
    command: str | list[str],
    working_dir: Path | None = None,
) -> None:
    """Execute the given command.

    Raises:
        Exception: If the the status code is non-zero.
    """
    status = execute_status(command, working_dir)
    if status:
        msg = f"Error status {status} while executing {command} in {working_dir}"
        raise Exception(msg)


def execute_status(
    command: str | list[str],
    working_dir: Path | None = None,
) -> int:
    """Execute the given command.

    Returns:
        The the status code.
    """
    if working_dir is not None:
        print(f"Executing in {working_dir}: {command}")
    else:
        print(f"Executing: {command}")
    command_line = shlex.split(command) if isinstance(command, str) else command

    return subprocess.call(command_line, cwd=working_dir)


# ---------------------------------------------------------------------------------

# The location the test site is built in.
DEV_SITE_URL = "https://checkerframework.org/dev"
DEV_SITE_DIR = Path("/cse/www2/types/checker-framework/dev")

# The location the test site is pushed to when it is ready.
LIVE_SITE_URL = "https://checkerframework.org"
LIVE_SITE_DIR = Path("/cse/www2/types/checker-framework")

# Per-user directory for the temporary files created by the release process.
# ("USER = os.getlogin()" does not work; see http://bugs.python.org/issue584566.
# Another alternative is: USER = os.getenv('USER').)
TMP_DIR = Path("/scratch") / pwd.getpwuid(os.geteuid())[0] / "cf-release"

# Location this and other release scripts are contained in.
SCRIPTS_DIR = TMP_DIR / "checker-framework" / "docs" / "developer" / "release"

# Location in which we will download files to run sanity checks.
SANITY_DIR = TMP_DIR / "sanity"

# The existence of this file indicates that release_build completed.
# It is deleted at the beginning of a release_build run, and at the
# end of a release_push run.
RELEASE_BUILD_COMPLETED_FLAG_FILE = TMP_DIR / "release-build-completed"

# Every time a release is built the changes/tags are pushed here.
INTERM_REPO_ROOT = TMP_DIR / "interm"
INTERM_CHECKER_REPO = INTERM_REPO_ROOT / "checker-framework"

# The central repositories for Checker Framework related projects.
LIVE_CHECKER_REPO = "git@github.com:typetools/checker-framework.git"
GIT_SCRIPTS_REPO = "https://github.com/plume-lib/git-scripts"
PLUME_SCRIPTS_REPO = "https://github.com/plume-lib/plume-scripts"
CHECKLINK_REPO = "https://github.com/plume-lib/checklink"
PLUME_BIB_REPO = "https://github.com/mernst/plume-bib"

# Location of the project directories in which we will build the actual projects.
# When we build these projects are pushed to the INTERM repositories.
BUILD_DIR = TMP_DIR / "build"
CHECKER_FRAMEWORK = BUILD_DIR / "checker-framework"
CHECKER_FRAMEWORK_RELEASE = CHECKER_FRAMEWORK / "docs" / "developer" / "release"

# If a new Gradle wrapper was recently installed, the first ./gradlew command may output:
#   Downloading https://services.gradle.org/distributions/gradle-6.6.1-bin.zip
execute("./gradlew version -q", TMP_DIR / "checker-framework")
CF_VERSION = execute_output("./gradlew version -q", TMP_DIR / "checker-framework").strip()

GIT_SCRIPTS = BUILD_DIR / "git-scripts"
PLUME_SCRIPTS = BUILD_DIR / "plume-scripts"
CHECKLINK = BUILD_DIR / "checklink"
PLUME_BIB = BUILD_DIR / "plume-bib"

INTERM_TO_BUILD_REPOS = ((INTERM_CHECKER_REPO, CHECKER_FRAMEWORK),)

LIVE_TO_INTERM_REPOS = ((LIVE_CHECKER_REPO, INTERM_CHECKER_REPO),)

CHECKER_LIVE_RELEASES_DIR = LIVE_SITE_DIR / "releases"
CHECKER_LIVE_API_DIR = LIVE_SITE_DIR / "api"

os.environ["PARENT_DIR"] = str(BUILD_DIR)
os.environ["CHECKERFRAMEWORK"] = str(CHECKER_FRAMEWORK)
perl_libs = f"{TMP_DIR}:/homes/gws/mernst/bin/src/perl:/usr/share/perl5/"
# Environment variables for tools needed during the build
os.environ["PLUME_SCRIPTS"] = str(PLUME_SCRIPTS)
os.environ["CHECKLINK"] = str(CHECKLINK)
os.environ["BIBINPUTS"] = ".:" + str(PLUME_BIB)
os.environ["TEXINPUTS"] = ".:/homes/gws/mernst/tex/sty:/homes/gws/mernst/tex:..:"
os.environ["PERLLIB"] = get_and_append("PERLLIB", ":") + perl_libs
os.environ["PERL5LIB"] = get_and_append("PERL5LIB", ":") + perl_libs
os.environ["JAVA_21_HOME"] = "/usr/lib/jvm/java-21-openjdk/"
os.environ["JAVA_HOME"] = os.environ["JAVA_21_HOME"]

EDITOR = os.getenv("EDITOR")
if EDITOR is None:
    EDITOR = "emacs"

PATH = os.environ["JAVA_HOME"] + "/bin:" + os.environ["PATH"]
PATH = PATH + ":/usr/bin"
PATH = PATH + ":" + str(PLUME_SCRIPTS)
PATH = PATH + ":" + str(CHECKLINK)
PATH = PATH + ":/homes/gws/mernst/.local/bin"  # for uv
PATH = PATH + ":."
os.environ["PATH"] = PATH

# Tools that must be on your PATH (besides common Unix ones like grep)
TOOLS = [
    "hevea",
    "perl",
    "java",
    "latex",
    "mvn",
    "git",
    "uv",
    "dot",
    EDITOR,
]
