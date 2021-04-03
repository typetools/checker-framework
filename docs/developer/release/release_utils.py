#!/usr/bin/env python3
# encoding: utf-8
"""
releaseutils.py

Python Utils for releasing the Checker Framework
This contains no main method only utility functions
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

import urllib.request
import urllib.error
import urllib.parse
import re
import subprocess
import os
import os.path
import shutil
from release_vars import execute

# =========================================================================================
# Parse Args Utils # TODO: Perhaps use argparse module


def read_command_line_option(argv, argument):
    """Returns True if the given command line arguments contain the specified
    argument, False otherwise."""
    for index in range(1, len(argv)):
        if argv[index] == argument:
            return True
    return False


# =========================================================================================
# Command utils


def execute_write_to_file(
    command_args, output_file_path, halt_if_fail=True, working_dir=None
):
    """Execute the given command, capturing the output to the given file."""
    print("Executing: %s" % (command_args))
    import shlex

    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    output_file = open(output_file_path, "w+")
    process = subprocess.Popen(
        args, stdout=output_file, stderr=output_file, cwd=working_dir
    )
    process.communicate()
    process.wait()
    output_file.close()

    if process.returncode != 0 and halt_if_fail:
        raise Exception(
            "Error %s while executing %s" % (process.returncode, command_args)
        )


def check_command(command):
    """Executes the UNIX \"which\" command to determine whether the given command
    is installed and on the PATH."""
    p = execute(["which", command], False)
    if p:
        raise AssertionError("command not found: %s" % command)
    print("")


def prompt_yes_no(msg, default=False):
    """Prints the given message and continually prompts the user until they
    answer yes or no. Returns true if the answer was yes, false otherwise."""
    default_str = "no"
    if default:
        default_str = "yes"

    result = prompt_w_default(msg, default_str, "^(Yes|yes|No|no)$")

    if result == "yes" or result == "Yes":
        return True
    return False


def prompt_yn(msg):
    """Prints the given message and continually prompts the user until they
    answer y or n. Returns true if the answer was y, false otherwise."""
    y_or_n = "z"
    while y_or_n != "y" and y_or_n != "n":
        print(msg + " [y|n]")
        y_or_n = input().lower()

    return y_or_n == "y"


def prompt_to_continue():
    "Prompts the user to continue, until they enter yes."
    while not prompt_yes_no("Continue?"):
        pass


def prompt_w_default(msg, default, valid_regex=None):
    """Only accepts answers that match valid_regex.
    If default is None, requires an answer."""
    answer = None
    while answer is None:
        answer = input(msg + " (%s): " % default)

        if answer is None or answer == "":
            answer = default
        else:
            answer = answer.strip()

            if valid_regex is not None:
                m = re.match(valid_regex, answer)
                if m is None:
                    answer = None
                    print("Invalid answer.  Validating regex: " + valid_regex)
            else:
                answer = default

    return answer


def check_tools(tools):
    """Given an array specifying a set of tools, verify that the tools are
    installed and on the PATH."""
    print("\nChecking to make sure the following programs are installed:")
    print(", ".join(tools))
    print(
        (
            "Note: If you are NOT working on buffalo.cs.washington.edu then you "
            + "likely need to change the variables that are set in release.py\n"
            + 'Search for "Set environment variables".'
        )
    )
    list(map(check_command, tools))
    print("")


def continue_or_exit(msg):
    "Prompts the user whether to continue executing the script."
    continue_script = prompt_w_default(
        msg + " Continue ('no' will exit the script)?", "yes", "^(Yes|yes|No|no)$"
    )
    if continue_script == "no" or continue_script == "No":
        raise Exception("User elected NOT to continue at prompt: " + msg)


# =========================================================================================
# Version Utils

# From http://stackoverflow.com/a/1714190/173852, but doesn't strip trailing zeroes
def version_number_to_array(version_num):
    """Given a version number, return an array of the elements, as integers."""
    return [int(x) for x in version_num.split(".")]


def increment_version(version_num):
    """
    Returns the next incremental version after the argument.
    """
    # Drop the fourth and subsequent parts if present
    version_array = version_number_to_array(version_num)[:3]
    version_array[-1] = version_array[-1] + 1
    return ".".join(str(x) for x in version_array)


def test_increment_version():
    """Run test cases to ensure that increment_version works correctly."""
    assert increment_version("1.0.3") == "1.0.4"
    assert increment_version("1.0.9") == "1.0.10"
    assert increment_version("1.1.9") == "1.1.10"
    assert increment_version("1.3.0") == "1.3.1"
    assert increment_version("1.3.1") == "1.3.2"
    assert increment_version("1.9.9") == "1.9.10"
    assert increment_version("3.6.22") == "3.6.23"
    assert increment_version("3.22.6") == "3.22.7"
    assert increment_version("1.0.3.1") == "1.0.4"
    assert increment_version("1.0.9.1") == "1.0.10"
    assert increment_version("1.1.9.1") == "1.1.10"
    assert increment_version("1.3.0.1") == "1.3.1"
    assert increment_version("1.3.1.1") == "1.3.2"
    assert increment_version("1.9.9.1") == "1.9.10"
    assert increment_version("3.6.22.1") == "3.6.23"
    assert increment_version("3.22.6.1") == "3.22.7"


def current_distribution_by_website(site):
    """
    Reads the checker framework version from the checker framework website and
    returns the version of the current release.
    """
    print("Looking up checker-framework-version from %s\n" % site)
    ver_re = re.compile(
        r"<!-- checker-framework-zip-version -->checker-framework-(.*)\.zip"
    )
    text = urllib.request.urlopen(url=site).read().decode("utf-8")
    result = ver_re.search(text)
    return result.group(1)


# =========================================================================================
# Git Utils


def git_bare_repo_exists_at_path(
    repo_root,
):  # Bare git repos have no .git directory but they have a refs directory
    "Returns whether a bare git repository exists at the given filesystem path."
    if os.path.isdir(repo_root + "/refs"):
        return True
    return False


def git_repo_exists_at_path(repo_root):
    """Returns whether a (bare or non-bare) git repository exists at the given
    filesystem path."""
    return os.path.isdir(repo_root + "/.git") or git_bare_repo_exists_at_path(repo_root)


def push_changes_prompt_if_fail(repo_root):
    """Attempt to push changes, including tags, that were committed to the
    repository at the given filesystem path. In case of failure, ask the user
    if they would like to try again. Loop until pushing changes succeeds or the
    user answers opts to not try again."""
    while True:
        cmd = "(cd %s && git push --tags)" % repo_root
        result = os.system(cmd)
        cmd = "(cd %s && git push origin master)" % repo_root
        result = os.system(cmd)
        if result == 0:
            break
        else:
            print(
                "Could not push from: "
                + repo_root
                + "; result="
                + str(result)
                + " for command: "
                + cmd
                + "` in "
                + os.getcwd()
            )
            if not prompt_yn(
                "Try again (responding 'n' will skip this push command but will not exit the script) ?"
            ):
                break


def push_changes(repo_root):
    """Pushes changes, including tags, that were committed to the repository at
    the given filesystem path."""
    execute("git push --tags", working_dir=repo_root)
    execute("git push origin master", working_dir=repo_root)


def update_repo(path, bareflag):
    """Pull the latest changes to the given repo and update. The bareflag
    parameter indicates whether the updated repo must be a bare git repo."""
    if bareflag:
        execute("git fetch origin master:master", working_dir=path)
    else:
        execute("git pull", working_dir=path)


def commit_tag_and_push(version, path, tag_prefix):
    """Commit the changes made for this release, add a tag for this release, and
    push these changes."""
    execute('git commit -a -m "new release %s"' % (version), working_dir=path)
    execute("git tag %s%s" % (tag_prefix, version), working_dir=path)
    push_changes(path)


def clone_from_scratch_or_update(src_repo, dst_repo, clone_from_scratch, bareflag):
    """If the clone_from_scratch flag is True, clone the given git or
    Mercurial repo from scratch into the filesystem path specified by dst_repo,
    deleting it first if the repo is present on the filesystem.
    Otherwise, if a repo exists at the filesystem path given by dst_repo, pull
    the latest changes to it and update it. If the repo does not exist, clone it
    from scratch. The bareflag parameter indicates whether the cloned/updated
    repo must be a bare git repo."""
    if clone_from_scratch:
        delete_and_clone(src_repo, dst_repo, bareflag)
    else:
        if os.path.exists(dst_repo):
            update_repo(dst_repo, bareflag)
        else:
            clone(src_repo, dst_repo, bareflag)


def delete_and_clone(src_repo, dst_repo, bareflag):
    """Clone the given git or Mercurial repo from scratch into the filesystem
    path specified by dst_repo. If a repo exists at the filesystem path given
    by dst_repo, delete it first. The bareflag parameter indicates whether
    the cloned repo must be a bare git repo."""
    delete_path_if_exists(dst_repo)
    clone(src_repo, dst_repo, bareflag)


def clone(src_repo, dst_repo, bareflag):
    """Clone the given git or Mercurial repo from scratch into the filesystem
    path specified by dst_repo. The bareflag parameter indicates whether the
    cloned repo must be a bare git repo."""
    flags = ""
    if bareflag:
        flags = "--bare"
    execute("git clone --quiet %s %s %s" % (flags, src_repo, dst_repo))


def is_repo_cleaned_and_updated(repo):
    """IMPORTANT: this function is not known to be fully reliable in ensuring
    that a repo is fully clean of all changes, such as committed tags. To be
    certain of success throughout the release_build and release_push process,
    the best option is to clone repositories from scratch.
    Returns whether the repository at the given filesystem path is clean (i.e.
    there are no committed changes and no untracked files in the working tree)
    and up-to-date with respect to the repository it was cloned from."""
    # The idiom "not execute(..., capture_output=True)" evaluates to True when the captured output is empty.
    if git_bare_repo_exists_at_path(repo):
        execute("git fetch origin", working_dir=repo)
        is_updated = not execute(
            "git diff master..FETCH_HEAD", working_dir=repo, capture_output=True
        )
        return is_updated
    else:
        # Could add "--untracked-files=no" to this command
        is_clean = not execute(
            "git status --porcelain", working_dir=repo, capture_output=True
        )
        execute("git fetch origin", working_dir=repo)
        is_updated = not execute(
            "git diff origin/master..master", working_dir=repo, capture_output=True
        )
        return is_clean and is_updated


def check_repos(repos, fail_on_error, is_intermediate_repo_list):
    """Fail if the repository is not clean and up to date."""
    for repo in repos:
        if git_repo_exists_at_path(repo):
            if not is_repo_cleaned_and_updated(repo):
                if is_intermediate_repo_list:
                    print(
                        (
                            "\nWARNING: Intermediate repository "
                            + repo
                            + " is not up to date with respect to the live repository.\n"
                            + "A separate warning will not be issued for a build repository that is cloned off of the intermediate repository."
                        )
                    )
                if fail_on_error:
                    raise Exception("repo %s is not cleaned and updated!" % repo)
                else:
                    if not prompt_yn(
                        "%s is not clean and up to date! Continue (answering 'n' will exit the script)?"
                        % repo
                    ):
                        raise Exception(
                            "%s is not clean and up to date! Halting!" % repo
                        )


def get_tag_line(lines, revision, tag_prefixes):
    """Get the revision hash for the tag matching the given project revision in
    the given lines containing revision hashes. Uses the given array of tag
    prefix strings if provided. For example, given an array of tag prefixes
    [\"checker-framework-\", \"checkers-\"] and project revision \"2.0.0\", the
    tags named \"checker-framework-2.0.0\" and \"checkers-2.0.0\" are sought."""
    for line in lines:
        for prefix in tag_prefixes:
            full_tag = prefix + revision
            if line.startswith(full_tag):
                return line
    return None


def get_commit_for_tag(revision, repo_file_path, tag_prefixes):
    """Get the commit hash for the tag matching the given project revision of
    the Git repository at the given filesystem path. Uses the given array of
    tag prefix strings if provided. For example, given an array of tag prefixes
    [\"checker-framework-\", \"checkers-\"] and project revision \"2.0.0\", the
    tags named \"checker-framework-2.0.0\" and \"checkers-2.0.0\" are sought."""

    # assume the first is the most recent
    tags = execute(
        "git rev-list " + tag_prefixes[0] + revision,
        True,
        True,
        working_dir=repo_file_path,
    )
    lines = tags.splitlines()

    commit = lines[0]
    if commit is None:
        msg = "Could not find revision %s in repo %s using tags %s " % (
            revision,
            repo_file_path,
            ",".join(tag_prefixes),
        )
        raise Exception(msg)

    return commit


# =========================================================================================
# File Utils


def wget_file(source_url, destination_dir):
    """Download a file from the source URL to the given destination directory.
    Useful since download_binary does not seem to work on source files."""
    print("DEST DIR: " + destination_dir)
    execute("wget %s" % source_url, True, False, destination_dir)


def download_binary(source_url, destination):
    """Download a file from the given URL and save its contents to the
    destination filename."""
    http_response = urllib.request.urlopen(url=source_url)
    content_length = http_response.headers["content-length"]

    if content_length is None:
        raise Exception("No content-length when downloading: " + source_url)

    dest_file = open(destination, "wb")
    dest_file.write(http_response.read())
    dest_file.close()


def read_first_line(file_path):
    "Return the first line in the given file. Assumes the file exists."
    infile = open(file_path, "r")
    first_line = infile.readline()
    infile.close()
    return first_line


def ensure_group_access(path):
    "Give group access to all files and directories under the specified path"
    # Errs for any file not owned by this user.
    # But, the point is to set group writeability of any *new* files.
    execute("chmod -f -R g+rw %s" % path, halt_if_fail=False)


def ensure_user_access(path):
    "Give the user access to all files and directories under the specified path"
    execute("chmod -f -R u+rwx %s" % path, halt_if_fail=True)


def set_umask():
    'Equivalent to executing "umask g+rw" from the command line.'
    os.umask(os.umask(0) & 0b001111)


def delete(file_to_delete):
    "Delete the specified file."
    os.remove(file_to_delete)


def delete_if_exists(file_to_delete):
    "Check if the specified file exists, and if so, delete it."
    if os.path.exists(file_to_delete):
        delete(file_to_delete)


def delete_path(path):
    "Delete all files and directories under the specified path."
    ensure_group_access(path)
    shutil.rmtree(path)


def delete_path_if_exists(path):
    """Check if the specified path exists, and if so, delete all files and
    directories under it."""
    if os.path.exists(path):
        delete_path(path)


def are_in_file(file_path, strs_to_find):
    """Returns true if every string in the given strs_to_find array is found in
    at least one line in the given file. In particular, returns true if
    strs_to_find is empty. Note that the strs_to_find parameter is mutated."""
    infile = open(file_path)

    for line in infile:
        if len(strs_to_find) == 0:
            return True

        index = 0
        while index < len(strs_to_find):
            if strs_to_find[index] in line:
                del strs_to_find[index]
            else:
                index = index + 1

    return len(strs_to_find) == 0


def insert_before_line(to_insert, file_path, line):
    """Insert the given line to the given file before the given 0-indexed line
    number."""
    mid_line = line - 1

    with open(file_path) as infile:
        content = infile.readlines()

    output = open(file_path, "w")
    for i in range(0, mid_line):
        output.write(content[i])

    output.write(to_insert)

    for i in range(mid_line, len(content)):
        output.write(content[i])

    output.close()


def create_empty_file(file_path):
    "Creates an empty file with the given filename."
    dest_file = open(file_path, "wb")
    dest_file.close()


# =========================================================================================
# Misc. Utils


def print_step(step):
    "Print a step in the release_build or release_push script."
    print("\n")
    print(step)

    dashStr = ""
    for dummy in range(0, len(step)):
        dashStr += "-"
    print(dashStr)


def get_announcement_email(version):
    """Return the template for the e-mail announcing a new release of the
    Checker Framework."""
    return """
To:  checker-framework-discuss@googlegroups.com
Subject: Release %s of the Checker Framework

We have released a new version of the Checker Framework.
The Checker Framework lets you create and/or run pluggable type checkers, in order to detect and prevent bugs in your code.

You can find documentation and download links at:
http://CheckerFramework.org/

Changes for Checker Framework version %s:

<<Insert latest Checker Framework changelog entry, omitting the first line with the release version and date, and with hard line breaks removed>>
""" % (
        version,
        version,
    )


# =========================================================================================
# Testing


def test_release_utils():
    "Test that critical methods in this file work as expected."
    test_increment_version()


# Tests run every time this file is loaded
test_release_utils()
