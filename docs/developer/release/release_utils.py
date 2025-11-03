#!/usr/bin/env python
"""Release utilities.

Python Utils for releasing the Checker Framework
This contains no main method only utility functions
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from __future__ import annotations

import os
import pathlib
import re
import shlex
import shutil
import subprocess
import urllib.request
from pathlib import Path

from release_vars import execute, execute_output, execute_status

# =========================================================================================
# Parse Args Utils # TODO: Perhaps use argparse module


def has_command_line_option(argv: list[str], argument: str) -> bool:
    """Return True if the given command line arguments contain the specified argument.

    Returns:
        True if the given command line arguments contain the specified argument.
    """
    return any(argv[index] == argument for index in range(1, len(argv)))


# =========================================================================================
# Command utils


def execute_write_to_file(
    command_args: str,
    output_file_path: Path,
    halt_if_fail: bool = True,
    working_dir: Path | None = None,
) -> None:
    """Execute the given command, capturing the output to the given file.

    Raises:
        Exception: If the command fails.
    """
    print(f"Executing: {command_args}")

    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    output_file = Path.open(output_file_path, "w+")
    process = subprocess.Popen(args, stdout=output_file, stderr=output_file, cwd=working_dir)
    process.communicate()
    process.wait()
    output_file.close()

    if process.returncode != 0 and halt_if_fail:
        msg = f"Error {process.returncode} while executing {command_args}"
        raise Exception(msg)


def check_command(command: str) -> None:
    """Throw an exception if the command is not on the PATH."""
    p = execute_status(["which", command])
    if p:
        raise AssertionError("command not found: " + command)
    print()


def prompt_yes_no(msg: str, default: bool = False) -> bool:
    """Print the given message and continually prompt the user until they answer yes or no.

    Returns:
        true if the answer was yes, false otherwise.
    """
    default_str = "no"
    if default:
        default_str = "yes"

    result = prompt_w_default(msg, default_str, "^(Yes|yes|No|no)$")

    return result == "yes" or result == "Yes"


def prompt_yn(msg: str) -> bool:
    """Print the given message and continually prompts the user until they answer y or n.

    Returns:
        true if the answer was y, false otherwise.
    """
    y_or_n = "z"
    while y_or_n != "y" and y_or_n != "n":
        print(msg + " [y|n]")
        y_or_n = input().lower()

    return y_or_n == "y"


def prompt_to_continue() -> None:
    """Prompts the user to continue, until they enter yes."""
    while not prompt_yes_no("Continue?"):
        pass


def prompt_w_default(msg: str, default: str, valid_regex: str | None = None) -> str:
    """Only accept answers that match valid_regex.  If default is None, require an answer.

    Returns:
        the user-provided input.
    """
    answer = None
    while answer is None:
        answer = input(msg + f" ({default}): ")

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


def check_tools(tools: list[str]) -> None:
    """Verify that each tools is on the PATH."""
    print("\nChecking to make sure the following programs are installed:")
    print(", ".join(tools))
    print(
        "Note: If you are NOT working on the CSE file system then you "
        "likely need to change the variables that are set in release.py\n"
        'Search for "Set environment variables".'
    )
    list(map(check_command, tools))
    print()


def continue_or_exit(msg: str) -> None:
    """Prompts the user whether to continue executing the script."""
    continue_script = prompt_w_default(
        msg + " Continue ('no' will exit the script)?", "yes", "^(Yes|yes|No|no)$"
    )
    if continue_script == "no" or continue_script == "No":
        raise Exception("User elected NOT to continue at prompt: " + msg)


# =========================================================================================
# Version Utils


# From http://stackoverflow.com/a/1714190/173852, but doesn't strip trailing zeroes
def version_number_to_array(version_num: str) -> list[int]:
    """Given a version number, return an array of the elements, as integers.

    Returns:
        an array of the version number elements.
    """
    return [int(x) for x in version_num.split(".")]


def increment_version(version_num: str) -> str:
    """Return the next incremental version after the argument.

    Returns:
        the next incremental version after the argument.
    """
    # Drop the fourth and subsequent parts if present
    version_array = version_number_to_array(version_num)[:3]
    version_array[-1] = version_array[-1] + 1
    return ".".join(str(x) for x in version_array)


def test_increment_version() -> None:
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


def current_distribution_by_website(site: str) -> str:
    """Return the Checker Framework version from the Checker Framework website.

    Returns:
        the Checker Framework version from the Checker Framework website.
    """
    print(f"Looking up checker-framework-version from {site}\n")
    ver_re = re.compile(r"<!-- checker-framework-zip-version -->checker-framework-(.*)\.zip")
    text = urllib.request.urlopen(url=site).read().decode("utf-8")
    result = ver_re.search(text)
    if result is None:
        raise Exception("Didn't find zip version in " + site)
    return result.group(1)


# =========================================================================================
# Git Utils


def git_bare_repo_exists_at_path(
    repo_root: Path,
) -> bool:
    """Return true if a bare git repository exists at the given filesystem path.

    Bare git repos have no .git directory but they have a refs directory.

    Returns:
        true if a bare git repository exists at the given filesystem path.
    """
    return (repo_root / "refs").is_dir()


def git_repo_exists_at_path(repo_root: Path) -> bool:
    """Return true if a (bare or non-bare) git repository exists at the given filesystem path.

    Returns:
        true if a (bare or non-bare) git repository exists at the given filesystem path.
    """
    return (repo_root / ".git").is_dir() or git_bare_repo_exists_at_path(repo_root)


def push_changes_prompt_if_fail(repo_root: Path) -> None:
    """Attempt to push changes, including tags, for the repository at the given filesystem path.

    In case of failure, ask the user
    if they would like to try again. Loop until pushing changes succeeds or the
    user answers opts to not try again.
    """
    while True:
        cmd = f"(cd {repo_root} && git push --tags)"
        result = os.system(cmd)
        cmd = f"(cd {repo_root} && git push origin master)"
        result = os.system(cmd)
        if result == 0:
            break
        print(
            f"Could not push from: {repo_root}; result={result} for command: `{cmd}`"
            f" in {pathlib.Path.cwd()}"
        )
        if not prompt_yn(
            "Try again (responding 'n' will skip this push command but will not exit the script) ?"
        ):
            break


def push_changes(repo_root: Path) -> None:
    """Push changes, including tags, for the repository at the given filesystem path."""
    execute("git push --tags", working_dir=repo_root)
    execute("git push origin master", working_dir=repo_root)


def update_repo(path: Path, bareflag: bool) -> None:
    """Pull the latest changes to the given repo and update.

    The bareflag parameter indicates whether the updated repo must be a bare git repo.
    """
    if bareflag:
        execute("git fetch origin master:master", working_dir=path)
    else:
        execute("git pull --ff-only", working_dir=path)


def commit_tag_and_push(version: str, path: Path, tag_prefix: str) -> None:
    """Commit the changes made for this release, add a tag, and push these changes."""
    # Do nothing (instead of erring) if there is nothing to commit.
    if execute_status("git diff-index --quiet HEAD", working_dir=path) != 0:
        execute(f'git commit -a -m "new release {version}"', working_dir=path)
    execute(f"git tag {tag_prefix}{version}", working_dir=path)
    push_changes(path)


def clone_from_scratch_or_update(
    src_repo: str | Path, dst_repo_dir: Path, clone_from_scratch: bool, bareflag: bool
) -> None:
    """Clone or update the repo.

    If the clone_from_scratch flag is True, clone the given git
    repo from scratch into the filesystem path specified by dst_repo_dir,
    deleting it first if the repo is present on the filesystem.
    Otherwise, if a repo exists at the filesystem path given by dst_repo_dir, pull
    the latest changes to it and update it. If the repo does not exist, clone it
    from scratch. The bareflag parameter indicates whether the cloned/updated
    repo must be a bare git repo.
    """
    if clone_from_scratch:
        delete_and_clone(str(src_repo), dst_repo_dir, bareflag)
    else:
        if pathlib.Path(dst_repo_dir).exists():
            update_repo(dst_repo_dir, bareflag)
        else:
            clone(str(src_repo), dst_repo_dir, bareflag)


def delete_and_clone(src_repo: str, dst_repo_dir: Path, bareflag: bool) -> None:
    """Clone the given git repo from scratch into the filesystem path specified by dst_repo_dir.

    If a repo exists at the filesystem path given
    by dst_repo_dir, delete it first. The bareflag parameter indicates whether
    the cloned repo must be a bare git repo.
    """
    delete_directory_if_exists(dst_repo_dir)
    clone(src_repo, dst_repo_dir, bareflag)


def clone(src_repo: str, dst_repo_dir: Path, bareflag: bool) -> None:
    """Clone the given git repo from scratch into the filesystem path specified by dst_repo_dir.

    The bareflag parameter indicates whether the cloned repo must be a bare git repo.
    """
    flags = ""
    if bareflag:
        flags = "--bare"
    execute(f"git clone --quiet {flags} {src_repo} {dst_repo_dir}")


def is_repo_cleaned_and_updated(repo_dir: Path) -> bool:
    """Return true if the repository at the given filesystem path is clean and up-to-date.

    Clean means there are no committed changes and no untracked files in the working tree.

    IMPORTANT: this function is not known to be fully reliable in ensuring
    that a repo is fully clean of all changes, such as committed tags. To be
    certain of success throughout the release_build and release_push process,
    the best option is to clone repositories from scratch.

    Returns:
        true if the repository at the given filesystem path is clean and up-to-date.
    """
    # The idiom "not execute_output(...)" evaluates to True when the captured output is empty.
    if git_bare_repo_exists_at_path(repo_dir):
        execute("git fetch origin", working_dir=repo_dir)
        is_updated = not execute_output("git diff master..FETCH_HEAD", working_dir=repo_dir)
        return is_updated
    # Could add "--untracked-files=no" to this command
    is_clean = not execute_output("git status --porcelain", working_dir=repo_dir)
    execute("git fetch origin", working_dir=repo_dir)
    is_updated = not execute_output("git diff origin/master..master", working_dir=repo_dir)
    return is_clean and is_updated


def check_repo(repo: Path, fail_on_error: bool, is_intermediate_repo_list: bool) -> None:
    """Fail if the repository is not clean and up to date."""
    if git_repo_exists_at_path(repo):
        if not is_repo_cleaned_and_updated(repo):
            if is_intermediate_repo_list:
                print(
                    f"\nWARNING: Intermediate repository {repo}"
                    " is not up to date with respect to the live repository.\n"
                    "A separate warning will not be issued for a build repository that is "
                    "cloned off of the intermediate repository."
                )
            if fail_on_error:
                raise Exception("repo " + str(repo) + " is not cleaned and updated!")
            if not prompt_yn(
                f"{repo} is not clean and up to date!"
                " Continue (answering 'n' will exit the script)?"
            ):
                raise Exception("repo " + str(repo) + " is not clean and up to date!")


def get_tag_line(lines: list[str], revision: str, tag_prefixes: list[str]) -> str | None:
    """Get the revision hash for the tag matching the given project revision.

    in
    the given lines containing revision hashes. Uses the given array of tag
    prefix strings if provided. For example, given an array of tag prefixes
    ["checker-framework-", "checkers-"] and project revision "2.0.0", the
    tags named "checker-framework-2.0.0" and "checkers-2.0.0" are sought.

    Returns:
        the revision hash for the tag matching the given project revision.
    """
    for line in lines:
        for prefix in tag_prefixes:
            full_tag = prefix + revision
            if line.startswith(full_tag):
                return line
    return None


def get_commit_for_tag(revision: str, repo_file_path: Path, tag_prefixes: list[str]) -> str:
    """Get the commit hash for the tag matching the given project revision.

    of
    the Git repository at the given filesystem path. Uses the given array of
    tag prefix strings if provided. For example, given an array of tag prefixes
    ["checker-framework-", "checkers-"] and project revision "2.0.0", the
    tags named "checker-framework-2.0.0" and "checkers-2.0.0" are sought.

    Returns:
        the commit hash for the tag matching the given project revision.
    """
    # assume the first is the most recent
    tags = execute_output(
        "git rev-list " + tag_prefixes[0] + revision,
        working_dir=repo_file_path,
    )
    lines = tags.splitlines()

    commit = lines[0]
    if commit is None:
        msg = (
            f"Could not find revision {revision} in repo {repo_file_path}"
            f" using tags {','.join(tag_prefixes)} "
        )
        raise Exception(msg)

    return commit


# =========================================================================================
# File Utils


def wget_file(source_url: str, destination_dir: Path) -> None:
    """Download a file from the source URL to the given destination directory.

    Useful since download_binary does not seem to work on source files.
    """
    print(f"DEST DIR: {destination_dir}")
    execute(f"wget {source_url}", destination_dir)


def download_binary(source_url: str, destination: Path) -> None:
    """Download a file from the given URL and save its contents to the destination filename."""
    http_response = urllib.request.urlopen(url=source_url)
    content_length = http_response.headers["content-length"]

    if content_length is None:
        raise Exception("No content-length when downloading: " + source_url)

    dest_file = Path.open(destination, "wb")
    dest_file.write(http_response.read())
    dest_file.close()


def read_first_line(file_path: Path) -> str:
    """Return the first line in the given file. Assumes the file exists.

    Returns:
        the first line in the given file.
    """
    infile = Path.open(file_path)
    first_line = infile.readline()
    infile.close()
    return first_line


def ensure_group_access(path: Path) -> None:
    """Give group access to all files and directories under the specified path."""
    # Errs for any file not owned by this user.
    # But, the point is to set group writeability of any *new* files.
    execute(f"chmod -f -R g+rw {path}")


def ensure_user_access(path: Path) -> None:
    """Give the user access to all files and directories under the specified path."""
    execute(f"chmod -f -R u+rwx {path}")


def set_umask() -> None:
    """Equivalent to executing "umask g+rw" from the command line."""
    os.umask(os.umask(0) & 0b001111)


def delete(file_to_delete: Path) -> None:
    """Delete the specified file."""
    pathlib.Path(file_to_delete).unlink()


def delete_if_exists(file_to_delete: Path) -> None:
    """Check if the specified file exists, and if so, delete it."""
    if pathlib.Path(file_to_delete).exists():
        delete(file_to_delete)


def delete_directory(path: Path) -> None:
    """Delete all files and directories under the specified path."""
    ensure_group_access(path)
    shutil.rmtree(path)


def delete_directory_if_exists(path: Path) -> None:
    """Delete all files and directories under the specified path, if it exists."""
    if pathlib.Path(path).exists():
        delete_directory(path)


def are_in_file(file_path: Path, strs_to_find: list[str]) -> bool:
    """Return true if every string in the given strs_to_find array appears in the given file.

    Note that the strs_to_find parameter is mutated.

    Returns:
        true if strs_to_find is empty.
    """
    infile = Path.open(file_path)

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


def insert_before_line(to_insert: str, file_path: Path, line: int) -> None:
    """Insert the given line to the given file before the given 0-indexed line number."""
    mid_line = line - 1

    with Path.open(file_path) as infile:
        content = infile.readlines()

    output = Path.open(file_path, "w")
    output.writelines(content[i] for i in range(mid_line))

    output.write(to_insert)

    output.writelines(content[i] for i in range(mid_line, len(content)))

    output.close()


def create_empty_file(file_path: Path) -> None:
    """Create an empty file with the given filename."""
    dest_file = Path.open(file_path, "wb")
    dest_file.close()


# =========================================================================================
# Misc. Utils


def print_step(step: str) -> None:
    """Print a step in the release_build or release_push script."""
    print("\n")
    print(step)

    dash_str = ""
    for _dummy in range(len(step)):
        dash_str += "-"
    print(dash_str)


def get_announcement_email(version: str) -> str:
    """Return the template for the e-mail announcing a new release of the Checker Framework.

    Returns:
        the template for the e-mail announcing a new release of the Checker Framework.
    """
    return f"""
To:  checker-framework-discuss@googlegroups.com
Subject: Release {version} of the Checker Framework

We have released a new version of the Checker Framework.
The Checker Framework lets you create and/or run pluggable type checkers, in order to detect and prevent bugs in your code.

You can find documentation and download links at:
http://CheckerFramework.org/

Changes for Checker Framework version {version}:

<<Insert latest Checker Framework changelog entry from https://github.com/typetools/checker-framework/blob/master/docs/CHANGELOG.md, preserving its formatting.>>
"""  # noqa: E501


# =========================================================================================
# Testing


def test_release_utils() -> None:
    """Test that critical methods in this file work as expected."""
    test_increment_version()


# Tests run every time this file is loaded
test_release_utils()
