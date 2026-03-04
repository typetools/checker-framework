#!/usr/bin/env python
"""Release the Checker Framework."""

# See README-release-process.html for more information

from __future__ import annotations

import datetime
import os
import shutil
import sys
from pathlib import Path

from release_utils import (  # ty: ignore # TODO: limitation in ty
    check_repo,
    check_tools,
    clone_from_scratch_or_update,
    commit_tag_and_push,
    continue_or_exit,
    create_empty_file,
    current_distribution_by_website,
    delete_directory_if_exists,
    delete_if_exists,
    ensure_group_access,
    ensure_user_access,
    has_command_line_option,
    increment_version,
    print_step,
    prompt_to_continue,
    prompt_w_default,
    prompt_yes_no,
    set_umask,
)
from release_vars import (  # ty: ignore # TODO: limitation in ty
    CF_VERSION,
    CHECKER_FRAMEWORK,
    CHECKLINK,
    CHECKLINK_REPO,
    DEV_SITE_DIR,
    INTERM_CHECKER_REPO,
    INTERM_TO_BUILD_REPOS,
    LIVE_SITE_URL,
    LIVE_TO_INTERM_REPOS,
    PLUME_BIB,
    PLUME_BIB_REPO,
    PLUME_SCRIPTS,
    PLUME_SCRIPTS_REPO,
    RELEASE_BUILD_COMPLETED_FLAG_FILE,
    TOOLS,
    execute,
    execute_status,
)

# Turned on by the --debug command-line option.
debug = False
ant_debug = ""


def print_usage() -> None:
    """Print usage information."""
    print("Usage:    python3 release_build.py [options]")
    print("\n  --debug  turns on debugging mode which produces verbose output")


def clone_or_update_repos() -> None:
    """Clone the relevant repos from scratch or update them if they exist.

    The action taken depends on a user query.
    """
    message = """Before building the release, we clone or update the release repositories.
However, if you have had to run the script multiple times today and no files
have changed since the last attempt, you may skip this step.
WARNING: IF THIS IS YOUR FIRST RUN OF THE RELEASE ON RELEASE DAY, DO NOT SKIP THIS STEP.
The following repositories will be cloned or updated from their origins:
"""
    for live_to_interm in LIVE_TO_INTERM_REPOS:
        message += str(live_to_interm[1]) + "\n"

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        message += str(interm_to_build[1]) + "\n"

    message += str(PLUME_SCRIPTS) + "\n"
    message += str(CHECKLINK) + "\n"
    message += str(PLUME_BIB) + "\n"

    message += "Clone repositories from scratch (answer no to get a chance to update them instead)?"

    clone_from_scratch = True

    if not prompt_yes_no(message, True):
        clone_from_scratch = False
        if not prompt_yes_no("Update the repositories without cloning them from scratch?", True):
            print("WARNING: Continuing without refreshing repositories.\n")
            return

    for live_to_interm in LIVE_TO_INTERM_REPOS:
        clone_from_scratch_or_update(live_to_interm[0], live_to_interm[1], clone_from_scratch, True)

    for interm_to_build in INTERM_TO_BUILD_REPOS:
        clone_from_scratch_or_update(
            interm_to_build[0], interm_to_build[1], clone_from_scratch, False
        )

    clone_from_scratch_or_update(PLUME_SCRIPTS_REPO, PLUME_SCRIPTS, clone_from_scratch, False)
    clone_from_scratch_or_update(CHECKLINK_REPO, CHECKLINK, clone_from_scratch, False)
    clone_from_scratch_or_update(PLUME_BIB_REPO, PLUME_BIB, clone_from_scratch, False)


def get_new_version(project_name: str, curr_version: str) -> tuple[str, str]:
    """Query the user for the new version number; returns old and new version numbers.

    Returns:
        the old and new version numbers
    """
    print("Current " + project_name + " version: " + curr_version)
    suggested_version = increment_version(curr_version)

    new_version = prompt_w_default(
        "Enter new version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$"
    )

    print("New version: " + new_version)

    if curr_version == new_version:
        curr_version = prompt_w_default(
            "Enter current version", suggested_version, "^\\d+\\.\\d+(?:\\.\\d+){0,2}$"
        )
        print("Current version: " + curr_version)

    return (curr_version, new_version)


def create_dir_for_dev_website_release_version(version: str) -> Path:
    """Create directory for CF project under the releases directory of the dev website.

    For example,
    /cse/www2/types/dev/checker-framework/checker-framework/releases/<version> .

    Returns:
        the dev web site directory for the CF.
    """
    interm_dir = Path(DEV_SITE_DIR) / "releases" / version
    delete_directory_if_exists(interm_dir)

    execute(f"mkdir -p {interm_dir}")
    return interm_dir


# def update_project_dev_website_symlink(project_name, release_version):
#     """Update the \"current\" symlink in the dev web site for the given project
#     to point to the given release of the project on the dev web site."""
#     project_dev_site = os.path.join(DEV_SITE_DIR, project_name)
#     link_path = os.path.join(project_dev_site, "current")
#
#     dev_website_relative_dir = os.path.join("releases", release_version)
#
#     print ("Writing symlink: " + link_path + "\nto point to relative directory: "
#             + dev_website_relative_dir)
#     force_symlink(dev_website_relative_dir, link_path)


def get_current_date() -> str:
    """Return today's date in the ISO format "2016-05-02".

    Returns:
        today's date.
    """
    return datetime.datetime.now().date().isoformat()  # noqa: DTZ005


def build_and_locally_deploy_maven() -> None:
    """Run `./gradlew publishToMavenLocal`."""
    execute("./gradlew publishToMavenLocal", working_dir=CHECKER_FRAMEWORK)


def build_checker_framework_release(
    version: str, old_cf_version: str, checker_framework_interm_dir: Path
) -> None:
    """Build the release files for the Checker Framework project and run tests.

    The release files include the manual and the zip file.
    """
    execute("./gradlew clean", working_dir=CHECKER_FRAMEWORK)

    # update versions
    execute("./gradlew updateVersionNumbers", working_dir=CHECKER_FRAMEWORK)

    # Check that updating versions didn't overlook anything.
    print("Here are occurrences of the old version number, " + old_cf_version + ":")
    grep_cmd = f"grep -n -r --exclude-dir=build --exclude-dir=.git -F {old_cf_version}"

    execute_status(grep_cmd, CHECKER_FRAMEWORK)
    continue_or_exit(
        "If any occurrence is not acceptable, then stop the release, update target"
        ' "updateVersionNumbers" in file release.gradle, and start over.'
    )

    # Build the Checker Framework binaries and documents.  Tests are run by release_push.py.
    # This also makes the manuals.
    gradle_cmd = "./gradlew buildAll"
    execute(gradle_cmd, CHECKER_FRAMEWORK)

    # make the checker framework tutorial
    checker_tutorial_dir = Path(CHECKER_FRAMEWORK) / "docs" / "tutorial"
    execute("make", checker_tutorial_dir)

    # Create checker-framework-X.Y.Z.zip and put it in checker_framework_interm_dir
    # copy the remaining checker-framework website files to checker_framework_interm_dir
    gradle_cmd = f"./gradlew copyToWebsite  -PcfWebsite={checker_framework_interm_dir}"
    execute(gradle_cmd, CHECKER_FRAMEWORK)

    # clean no longer necessary files left over from building the checker framework tutorial
    checker_tutorial_dir = Path(CHECKER_FRAMEWORK) / "docs" / "tutorial"
    execute("make clean", checker_tutorial_dir)

    build_and_locally_deploy_maven()

    dev_website_relative_dir = Path(DEV_SITE_DIR) / "releases" / version
    print(f"Copying from: {dev_website_relative_dir}\n  to: {DEV_SITE_DIR}")
    ensure_group_access(dev_website_relative_dir)
    ensure_user_access(dev_website_relative_dir)
    shutil.copytree(str(dev_website_relative_dir), str(DEV_SITE_DIR), dirs_exist_ok=True)


def commit_to_interm_projects(cf_version: str) -> None:
    """Commit the changes for each project from its build repo to its intermediate repo.

    This is in preparation for running the release_push
    script, which does not read the build repos.
    """
    # Use project definition instead, see find project location find_project_locations

    commit_tag_and_push(cf_version, CHECKER_FRAMEWORK, "checker-framework-")


def main(argv: list[str]) -> None:
    """Build the release artifacts for the Checker Framework project.

    Also place them in the development web site. It can also be used to review
    the documentation and changelogs for the three projects.
    """
    # MANUAL Indicates a manual step
    # AUTO Indicates the step is fully automated.

    delete_if_exists(RELEASE_BUILD_COMPLETED_FLAG_FILE)

    set_umask()

    global debug, ant_debug
    debug = has_command_line_option(argv, "--debug")
    if debug:
        ant_debug = "-debug"

    # For each project, build what is necessary but don't push

    print("Building a new release of the Checker Framework!")

    print("\nPATH:\n" + os.environ["PATH"] + "\n")

    print_step("Build Step 1: Clone the build and intermediate repositories.")  # MANUAL

    # Recall that there are 3 relevant sets of repositories for the release:
    # * build repository - repository where the project is built for release
    # * intermediate repository - repository to which release related changes are pushed
    #   after the project is built
    # * release repository - GitHub repositories, the central repository.

    # Every time we run release_build, changes are committed to the intermediate repository from
    # build but NOT to the release repositories. If we are running the build script multiple times
    # without actually committing the release then these changes need to be cleaned before we run
    # the release_build script again.  The "Clone/update repositories" step updates the repositories
    # with respect to the live repositories on GitHub, but it is the "Verify repositories" step that
    # ensures that they are clean, i.e. indistinguishable from a freshly cloned repository.

    # check we are cloning LIVE -> INTERM, INTERM -> RELEASE
    print_step("\nStep 1a: Clone/update repositories.")  # MANUAL
    clone_or_update_repos()

    # This step ensures the previous step worked. It checks to see if we have any modified files,
    # untracked files, or outgoing changesets. If so, it fails.

    print_step("Step 1b: Verify repositories.")  # MANUAL
    check_repo(CHECKER_FRAMEWORK, True, True)
    check_repo(INTERM_CHECKER_REPO, True, False)

    # The release script requires a number of common tools (Ant, Maven, make, etc...). This step
    # checks to make sure all tools are available on the command line in order to avoid wasting time
    # in the event a tool is missing late in execution.

    print_step("Build Step 2: Check tools.")  # AUTO
    check_tools(TOOLS)

    # Usually we increment the release by 0.0.1 per release unless there is a major change.  The
    # release script will read the current version of the Checker Framework from the release
    # website and then suggest the next release version 0.0.1 higher than the current version. You
    # can also manually specify a version higher than the current version. Lower or equivalent
    # versions are not possible and will be rejected when you try to push the release.

    print_step("Build Step 3: Determine release versions.")  # MANUAL

    old_cf_version = current_distribution_by_website(LIVE_SITE_URL)
    cf_version = CF_VERSION
    print("Version: " + cf_version + "\n")

    if old_cf_version == cf_version:
        print(
            "It is *strongly discouraged* to not update the release version numbers for "
            "the Checker Framework even if no changes were made to these in a month. "
            "This would break so much in the release scripts that they would become unusable. "
            "Update the version number in checker-framework/build.gradle\n"
        )
        prompt_to_continue()

    print_step("Build Step 4: Create directories for the current release on the dev site.")  # AUTO

    checker_framework_interm_dir = create_dir_for_dev_website_release_version(cf_version)

    # The Checker Framework jar files and documentation are built and the website is updated.
    print_step("Build Step 5: Build projects and websites.")  # AUTO
    build_checker_framework_release(cf_version, old_cf_version, checker_framework_interm_dir)

    print_step("Build Step 6: Overwrite .htaccess and CFLogo.png .")  # AUTO

    # Not "cp -p" because that does not work across filesystems whereas rsync does
    cf_logo = Path(CHECKER_FRAMEWORK) / "docs" / "logo" / "Logo" / "CFLogo.png"
    execute(f"rsync --times {cf_logo} {checker_framework_interm_dir}")

    # Each project has a set of files that are updated for release. Usually these updates include
    # new release date and version information. All changed files are committed and pushed to the
    # intermediate repositories. Keep this in mind if you have any changed files from steps 1d, 4,
    # or 5. Edits to the scripts in the cf-release/scripts directory will never be checked in.

    print_step("Build Step 7: Commit projects to intermediate repos.")  # AUTO
    commit_to_interm_projects(cf_version)

    # Adds read/write/execute group permissions to all of the new dev website directories
    # under https://checkerframework.org/dev/ These directories need group read/execute
    # permissions in order for them to be served.

    print_step("\n\nBuild Step 8: Add group permissions to repos.")
    ensure_group_access(CHECKER_FRAMEWORK)
    ensure_group_access(INTERM_CHECKER_REPO)

    print_step("\n\nBuild Step 9: Add group permissions to websites.")  # AUTO
    ## TODO: This is returning a status of 1, but it runs fine from the command line.
    # ensure_group_access(DEV_SITE_DIR)

    create_empty_file(RELEASE_BUILD_COMPLETED_FLAG_FILE)


if __name__ == "__main__":
    main(sys.argv)
