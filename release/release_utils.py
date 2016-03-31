#!/usr/bin/env python
# encoding: utf-8
"""
releaseutils.py

Python Utils for releasing the Checker Framework
This contains no main method only utility functions
Created by Jonathan Burke 11/21/2012

Copyright (c) 2012 University of Washington
"""

from xml.dom import minidom
import sys
import urllib2
import re
import subprocess
from subprocess import Popen, PIPE
import os
import os.path
import shutil
import errno
import datetime
from release_vars  import *

#=========================================================================================
# Parse Args Utils # TODO: Perhaps use argparse module
def match_arg(arg):
    matched_project = None
    for project in PROJECTS_TO_SHORTNAMES:
        if arg == project[0] or arg == project[1]:
            matched_project = project
    return matched_project

def read_projects(argv, error_call_back):
    matched_projects = {
        LT_OPT  : False,
        AFU_OPT : False,
        CF_OPT  : False
    }

    arg_length = len(sys.argv)

    if arg_length < 2:
        print "You  must select at least one project!"
        error_call_back()
        sys.exit(1)

    error = False
    for index in range(1, arg_length):
        arg = argv[index]

        if arg == ALL_OPT:
            for project in PROJECTS_TO_SHORTNAMES:
                matched_projects[project[0]] = True
            return matched_projects

        matched_project = match_arg(argv[index])
        if matched_project is None:
            print  "Unmatched project: " + argv[index]
            error = True
        else:
            matched_projects[matched_project[0]] = True

    if error:
        error_call_back()
        sys.exit(1)

    return matched_projects

def add_project_dependencies(matched_projects):
    if matched_projects[CF_OPT]:
        matched_projects[AFU_OPT] = True
        matched_projects[LT_OPT] = True
    else:
        if matched_projects[AFU_OPT]:
            matched_projects[LT_OPT] = True


def print_projects(print_error_label, indent_level, indent_size):
    indentation = duplicate(duplicate(" ", indent_size), indent_level)
    project_to_short_cols = 27

    if print_error_label:
        print "projects:   You must specify at least one of the following projects or \"all\""

    print  indentation + pad_to("project", " ", project_to_short_cols) + "short-name"

    for project in PROJECTS_TO_SHORTNAMES:
        print indentation + pad_to(project[0], " ", project_to_short_cols) + project[1]

    print indentation + ALL_OPT

def duplicate(string, times):
    result = ""
    for dummy in range(0, times):
        result += string
    return result

def pad_to(original_str, filler, size):
    missing = size - len(original_str)
    return original_str + duplicate(filler, missing)

def read_auto(argv):
    for index in range(1, len(argv)):
        if argv[index] == "--auto":
            return True

    return False

def read_review_manual(argv):
    for index in range(1, len(argv)):
        if argv[index] == "--review-manual":
            return True

    return False

#=========================================================================================
# Command utils

def execute(command_args, halt_if_fail=True, capture_output=False, working_dir=None):
    """Execute the given command.
If capture_output is true, then return the output (and ignore the halt_if_fail argument).
If capture_output is not true, return the return code of the subprocess call."""

    print "Executing: %s" % (command_args)
    import shlex
    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    if capture_output:
        process = subprocess.Popen(args, stdout=subprocess.PIPE, cwd=working_dir)
        out = process.communicate()[0]
        process.wait()
        return out

    else:
        result = subprocess.call(args, cwd=working_dir)
        if halt_if_fail and result:
            raise Exception('Error %s while executing %s' % (result, command_args))
        return result

def execute_write_to_file(command_args, output_file_path, halt_if_fail=True, working_dir=None):
    print "Executing: %s" % (command_args)
    import shlex
    args = shlex.split(command_args) if isinstance(command_args, str) else command_args

    output_file = open(output_file_path, 'w+')
    process = subprocess.Popen(args, stdout=output_file, stderr=output_file, cwd=working_dir)
    process.communicate()
    process.wait()
    output_file.close()

    if process.returncode != 0 and halt_if_fail:
        raise Exception('Error %s while executing %s' % (process.returncode, command_args))

def check_command(command):
    p = execute(['which', command], False)
    if p:
        raise AssertionError('command not found: %s' % command)
    print ''

def prompt_yes_no(msg, default=False):
    default_str = "no"
    if default:
        default_str = "yes"

    result = prompt_w_suggestion(msg, default_str, "^(Yes|yes|No|no)$")
    return is_yes(result)

def prompt_yn(msg):
    y_or_n = 'z'
    while y_or_n != 'y' and y_or_n != 'n':
        print msg + " [y|n]"
        y_or_n = raw_input().lower()

    return y_or_n == 'y'

def maybe_prompt_yn(msg, prompt):
    if not prompt:
        return True

    return prompt_yn(msg)

def prompt_until_yes():
    while not prompt_yes_no("Continue?"):
        pass

def prompt_w_suggestion(msg, suggestion, valid_regex=None):
    "Only accepts answers that match valid_regex."
    answer = None
    while answer is None:
        answer = raw_input(msg + " (%s): " % suggestion)

        if answer is None or answer == "":
            answer = suggestion
        else:
            answer = answer.strip()

            if valid_regex is not None:
                m = re.match(valid_regex, answer)
                if m is None:
                    answer = None
                    print "Invalid answer.  Validating regex: " + valid_regex
            else:
                answer = suggestion

    return answer

def maybe_prompt_w_suggestion(msg, suggestion, valid_regex, prompt):
    if not prompt:
        return True
    return prompt_w_suggestion(msg, suggestion, valid_regex, prompt)

def check_tools(tools):
    print "\nChecking to make sure the following programs are installed:"
    print ', '.join(tools)
    print('Note: If you are NOT working on buffalo.cs.washington.edu then you ' +
          'likely need to change the variables that are set in release.py\n' +
          'Search for "Set environment variables".')
    map(check_command, tools)
    print ''

def match_one(to_test, pattern_strings):
    for pattern_string in pattern_strings:
        is_match = re.match(pattern_string, to_test)
        if is_match is not None:
            return pattern_string

    return None

#=========================================================================================
# Version Utils

# From http://stackoverflow.com/a/1714190/173852, but doesn't strip trailing zeroes
def version_number_to_array(version_num):
    """Given a version number, return an array of the elements, as integers."""
    return [int(x) for x in version_num.split(".")]

def version_array_to_string(version_array):
    return ".".join(str(x) for x in version_array)

# From http://stackoverflow.com/a/1714190/173852
def compare_version_numbers(version1, version2):
    return cmp(version_number_to_array(version1), version_number_to_array(version2))

def max_version(strs):
    """Given a list of version number strings, returns the largest one."""
    return max(strs, key=version_number_to_array)

def test_max_version():
    assert max_version(["3.1.4", "4.2"]) == '4.2'
    assert max_version(["3.1.4", "14.2"]) == '14.2'

def increment_version(version_num, single_digits=False):
    """
    Returns the next incremental version after the argument.
    If single_digits is true, do not permit any part to grow greater than 9.
    """
    # Drop the fourth and subsequent parts if present
    version_array = version_number_to_array(version_num)[:3]
    version_array[-1] = version_array[-1] + 1
    if single_digits and version_array[-1] > 9:
        return increment_version(version_array_to_string(version_array[0:-1]), single_digits) + ".0"
    return version_array_to_string(version_array)

def test_increment_version():
    assert increment_version('1.0.3') == '1.0.4'
    assert increment_version('1.0.9') == '1.0.10'
    assert increment_version('1.1.9') == '1.1.10'
    assert increment_version('1.3.0') == '1.3.1'
    assert increment_version('1.3.1') == '1.3.2'
    assert increment_version('1.9.9') == '1.9.10'
    assert increment_version('3.6.22') == '3.6.23'
    assert increment_version('3.22.6') == '3.22.7'
    assert increment_version('1.0.3.1') == '1.0.4'
    assert increment_version('1.0.9.1') == '1.0.10'
    assert increment_version('1.1.9.1') == '1.1.10'
    assert increment_version('1.3.0.1') == '1.3.1'
    assert increment_version('1.3.1.1') == '1.3.2'
    assert increment_version('1.9.9.1') == '1.9.10'
    assert increment_version('3.6.22.1') == '3.6.23'
    assert increment_version('3.22.6.1') == '3.22.7'
    assert increment_version('1.0.3', True) == '1.0.4'
    assert increment_version('1.0.9', True) == '1.1.0'
    assert increment_version('1.1.9', True) == '1.2.0'
    assert increment_version('1.3.0', True) == '1.3.1'
    assert increment_version('1.3.1', True) == '1.3.2'
    assert increment_version('1.9.9', True) == '2.0.0'
    assert increment_version('3.6.22', True) == '3.7.0'
    assert increment_version('3.22.6', True) == '3.22.7'


def current_distribution_by_website(site):
    """
    Reads the checker framework version from the checker framework website and
    returns the version of the current release
    """
    print 'Looking up checker-framework-version from %s\n' % site
    ver_re = re.compile(r"<!-- checker-framework-version -->(.*),")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)

def current_distribution(checker_framework_dir):
    """
    Reads the checker framework version from build-common.properties
    returns the version of the current release
    """
    ver_re = re.compile(r"""build.version = (\d+\.\d+\.\d+(?:\.\d+){0,1})""")
    build_props_location = os.path.join(checker_framework_dir, "build-common.properties")
    build_props = open(build_props_location)

    for line in build_props:
        match = ver_re.search(line)
        if match:
            return match.group(1)

    print  "Couldn't find checker framework version in file: " + build_props_location
    sys.exit(1)

def extract_from_site(site, open_tag, close_tag):
    """
    Reads a string from between open and close tag at the given url
    """
    regex_str = open_tag + "(.*)" + close_tag

    ver_re = re.compile(regex_str)
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)


def latest_openjdk(site):
    ver_re = re.compile(r"Build b(\d+)")
    text = urllib2.urlopen(url=site).read()
    result = ver_re.search(text)
    return result.group(1)


def find_latest_version(version_dir):
    return max_version(filter(os.path.isdir, os.listdir(version_dir)))

def get_afu_version_from_html(html_file_path):
    version_regex = "<!-- afu-version -->(\\d+\\.\\d+\\.?\\d*),.*<!-- /afu-version -->"
    version = find_first_instance(version_regex, html_file_path, "")
    if version is None:
        raise Exception("Could not detect Annotation File Utilities version in file " + html_file_path)

    return version

#=========================================================================================
# Mercurial Utils

def is_git(repo_root):
    return is_git_private(repo_root, True)

def is_git_not_bare(repo_root):
    return is_git_private(repo_root, False)

def is_git_private(repo_root, return_value_on_bare):
    if git_bare_repo_exists_at_path(repo_root):
        return return_value_on_bare
    if git_repo_exists_at_path(repo_root):
        return True
    if hg_repo_exists_at_path(repo_root):
        return False
    raise Exception(repo_root + " is not recognized as a git, git bare, or hg repository")

def git_bare_repo_exists_at_path(repo_root): # Bare git repos have no .git directory but they have a refs directory
    if os.path.isdir(repo_root + "/refs"):
        return True
    return False

def git_repo_exists_at_path(repo_root):
    return os.path.isdir(repo_root + "/.git") or git_bare_repo_exists_at_path(repo_root)

def hg_repo_exists_at_path(repo_root):
    return os.path.isdir(repo_root + "/.hg")

def hg_push_or_fail(repo_root):
    if is_git(repo_root):
        cmd = 'git -C %s push --tags' % repo_root
        result = os.system(cmd)
        if not result == 0:
            raise Exception("Could not push tags to: " + repo_root + "; result=" + result + " for command: " + cmd)
    if is_git(repo_root):
        cmd = 'git -C %s push' % repo_root
    else:
        cmd = 'hg -R %s push' % repo_root
    result = os.system(cmd)
    if not result == 0:
        raise Exception("Could not push to: " + repo_root + "; result=" + result + " for command: " + cmd)

def hg_push(repo_root):
    if is_git(repo_root):
        execute('git -C %s push --tags' % repo_root)
        execute('git -C %s push' % repo_root)
    else:
        execute('hg -R %s push' % repo_root)

# Pull the latest changes and update
def update_project(path):
    if git_bare_repo_exists_at_path(path):
        execute('git -C %s fetch' % path)
    elif is_git(path):
        execute('git -C %s pull' % path)
    else:
        execute('hg -R %s pull -u' % path)

def update_projects(paths):
    for path in paths:
        update_project(path)
        # print "Checking changes"
        # execute('hg -R %s outgoing' % path)

# Commit the changes we made for this release
# Then add a tag for this release
# And push these changes
def commit_tag_and_push(version, path, tag_prefix):
    if is_git(path):
        execute('git -C %s commit -a -m "new release %s"' % (path, version))
        execute('git -C %s tag %s%s' % (path, tag_prefix, version))
    else:
        execute('hg -R %s commit -m "new release %s"' % (path, version))
        execute('hg -R %s tag %s%s' % (path, tag_prefix, version))
    hg_push(path)

# Retrieve the changes since the tag (prefix + prev_version)
def retrieve_changes(root, prev_version, prefix):
    if is_git(root):
        cmd_template = "git -C %s log %s%s.."
    else:
        cmd_template = "hg -R %s log -r %s%s:tip --template ' * {desc}\n'"
    return execute(cmd_template % (root, prefix, prev_version),
                   capture_output=True)

def delete_and_clone(src_repo, dst_repo, bareflag):
    if os.path.exists(dst_repo):
        delete_path(dst_repo)

    clone(src_repo, dst_repo, bareflag)

def clone(src_repo, dst_repo, bareflag):
    isGitRepo = False
    if "http" in src_repo:
        if "git" in src_repo:
            isGitRepo = True
    elif is_git(src_repo):
        isGitRepo = True

    if isGitRepo:
        flags = ""
        if bareflag:
            flags = "--bare"
        execute('git clone --quiet %s %s %s' % (flags, src_repo, dst_repo))
    else:
        execute('hg clone --quiet %s %s' % (src_repo, dst_repo))

def is_repo_cleaned_and_updated(repo):
    if is_git(repo):
        # The idiom "not execute(..., capture_output=True)" evaluates to True when the captured output is empty.
        if git_bare_repo_exists_at_path(repo):
            execute("git -C %s fetch origin" % (repo))
            is_updated = not execute("git -C %s diff master..FETCH_HEAD" % (repo), capture_output=True)
            return is_updated
        else:
            # Could add "--untracked-files=no" to this command
            is_clean = not execute("git -C %s status --porcelain" % (repo), capture_output=True)
            execute("git -C %s fetch origin" % (repo))
            is_updated = not execute("git -C %s diff origin/master..master" % (repo), capture_output=True)
            return is_clean and is_updated
    else:
        summary = execute('hg -R %s summary --remote' % (repo), capture_output=True)
        if "commit: (clean)" not in summary:
            return False
        if "update: (current)" not in summary:
            return False
        if "remote: (synced)" not in summary:
            return False
        return True

def repo_exists(repo_root):
    return git_repo_exists_at_path(repo_root) or hg_repo_exists_at_path(repo_root)

def strip(repo):
    """Deletes all outgoing changesets"""
    if git_bare_repo_exists_at_path(repo):
        raise Exception("strip cannot be called on a bare repo (%s)" % repo)
    elif is_git(repo):
        execute("git -C %s reset --hard origin/master" % repo)
        return
    cmd = ['hg', '-R', repo, 'strip', '--no-backup', 'roots(outgoing())']
    print "Executing: " + " ".join(cmd)
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    out, err = process.communicate()
    process.wait()

    if out:
        match = re.search(r"\d+ files updated, \d+ files merged, \d+ files removed, \d+ files unresolved", out)
        if match is None:
            match = re.search("empty revision set", err)
            if match is None:
                raise Exception("Could not recognize strip output: (%s, %s, %s)"
                                % (process.returncode, out, err))
            else:
                print err
        else:
            print out
    print ""

def revert(repo):
    if git_bare_repo_exists_at_path(repo):
        raise Exception("revert cannot be called on a bare repo (%s)" % repo)
    if is_git(repo):
        execute('git -C %s reset --hard' % repo)
    else:
        execute('hg -R %s revert --all' % repo)

def purge(repo, ignored_files=False):
    """Delete untracked files from the working directory, like "hg purge".
If "ignored_files" argument is true, also delete ignored files."""
    if git_bare_repo_exists_at_path(repo):
        raise Exception("purge cannot be called on a bare repo (%s)" % repo)
    if is_git(repo):
        if ignored_files:
            cmd = 'git -C %s clean -f -x' % repo
        else:
            cmd = 'git -C %s clean -f' % repo
    else:
        if ignored_files:
            cmd = 'hg -R %s purge  --all' % repo
        else:
            cmd = 'hg -R %s purge' % repo
    execute(cmd)

def clean_repo(repo, prompt):
    if maybe_prompt_yn('Remove all modified files, untracked files and outgoing commits from %s ?' % repo, prompt):
        ensure_group_access(repo)
        if git_bare_repo_exists_at_path(repo):
            return
        revert(repo)
        strip(repo)
        purge(repo, all)
        revert(repo) # avoids the issue of purge deleting ignored files we want to get back
    print ''

def clean_repos(repos, prompt):
    if maybe_prompt_yn('Remove all modified files, untracked files and outgoing commits from:\n%s ?' % '\n'.join(repos), prompt):
        for repo in repos:
            if repo_exists(repo):
                clean_repo(repo, False)

def check_repos(repos, fail_on_error, is_intermediate_repo_list):
    """Fail if the repository is not clean and up to date."""
    for repo in repos:
        if repo_exists(repo):
            if not is_repo_cleaned_and_updated(repo):
                if is_intermediate_repo_list:
                    print("\nWARNING: Intermediate repository " + repo + " is not up to date with respect to the live repository.\n" +
                          "A separate warning will not be issued for a build repository that is cloned off of the intermediate repository.")
                if fail_on_error:
                    raise Exception('repo %s is not cleaned and updated!' % repo)
                else:
                    if not prompt_yn('%s is not clean and up to date! Continue?' % repo):
                        raise Exception('%s is not clean and up to date! Halting!' % repo)

def get_tag_line(lines, revision, tag_prefixes):
    for line in lines:
        for prefix in tag_prefixes:
            full_tag = prefix + revision
            if line.startswith(full_tag):
                return line
    return None

def get_commit_line_for_tag(lines, revision, tag_prefixes):
    last_commit = None

    for line in lines:
        if line.startswith("commit "):
            last_commit = line.split(" ")[1]
        else:
            for prefix in tag_prefixes:
                full_tag = prefix + revision
                if full_tag in line:
                    return last_commit
    return None

def get_commit_for_tag(revision, repo_file_path, tag_prefixes):
    if not is_git(repo_file_path):
        raise Exception("get_commit_for_tag is only defined for git repositories")

    # assume the first is the most recent
    tags = execute("git -C " + repo_file_path + " rev-list " + tag_prefixes[0] + revision, True, True)
    lines = tags.splitlines()

    commit = lines[0]
    if commit is None:
        msg = "Could not find revision %s in repo %s using tags %s " %  (revision, repo_file_path, ",".join(tag_prefixes))
        raise Exception(msg)

    return commit


def get_hash_for_tag(revision, repo_file_path, tag_prefixes):
    if is_git(repo_file_path):
        raise Exception("get_hash_for_tag is not defined for git repositories")
    tags = execute("hg tags -R " + repo_file_path, True, True)
    lines = tags.split("\n")

    target = get_tag_line(lines, revision, tag_prefixes)
    if target is None:
        msg = "Could not find revision %s in repo %s using tags %s " %  (revision, repo_file_path, ",".join(tag_prefixes))
        raise Exception(msg)

    tokens = target.split()
    result = tokens[1].split(":")[1]
    return result

def get_tip_hash(repository):
    return get_hash_for_tag("tip", repository, [""])

def write_changesets_since(old_version, repository, tag_prefixes, outfile):
    if is_git(repository):
        old_tag = get_commit_for_tag(old_version, repository, tag_prefixes)
        cmd = "git -C %s log %s.." % (repository, old_tag)
    else:
        old_tag = get_hash_for_tag(old_version, repository, tag_prefixes)
        tip_tag = get_tip_hash(repository)

        cmd = "hg -R %s log -r%s:%s" % (repository, old_tag, tip_tag)

    execute_write_to_file(cmd, outfile)

def write_diff_to_file(old_version, repository, tag_prefixes, dir_path, outfile):
    if is_git(repository):
        old_tag = get_commit_for_tag(old_version, repository, tag_prefixes)
        cmd = "git -C %s diff -w %s.. %s" % (repository, old_tag, dir_path)
    else:
        old_tag = get_hash_for_tag(old_version, repository, tag_prefixes)
        tip_tag = get_tip_hash(repository)
        cmd = "hg -R %s diff -w -r%s:%s %s" % (repository, old_tag, tip_tag, dir_path)
    execute_write_to_file(cmd, outfile)

def propose_change_review(dir_title, old_version, repository_path, tag_prefixes,
                          dir_path, diff_output_file):
    if prompt_yes_no("Review %s?" %dir_title, True):
        write_diff_to_file(old_version, repository_path, tag_prefixes, dir_path, diff_output_file)
        # A side effect here is that the user will see updated version numbers in this diff that won't
        # be reflected in their local repository.  I think that is OK.  The version numbers will be updated
        # when the actual release is made anyway.
        # Alternatively the process could be modified such that the version number updates are not made
        # before the user sees the diff. However that would mean the user never gets a chance to review
        # those updates.
        print  "Please review " + dir_title + " and make any edits you deem necessary in your local clone of the repository."
        print  "Diff file: " + diff_output_file
        prompt_until_yes()

#=========================================================================================
# File Utils

# since download_binary does not seem to work on source files
def wget_file(source_url, destination_dir):
    print "DEST DIR: " + destination_dir
    execute("wget %s" % source_url, True, False, destination_dir)

# Note:  This will download the directory into a directory location as follow:
# Suppose we have a url:  http://level0/level1/target
# It will download the files into the following directory
# destination_dir/level0/level1/target
# use wget_dir_flat if you'd like all files to be just output to destination_dir
def wget_dir(source_url, destination_dir):
    execute("wget -r -l1 --no-parent %s" % source_url, True, False, destination_dir)

def wget_dir_flat(source_url, destination_dir):
    execute("wget -r -l1 --no-parent -nd %s" % source_url, True, False, destination_dir)

def download_binary(source_url, destination, max_size):
    http_response = urllib2.urlopen(url=source_url)
    content_length = http_response.headers['content-length']

    if content_length is None:
        raise Exception("No content-length when downloading: " + source_url)

    if int(content_length) > max_size:
        raise Exception("Content-length (" + content_length + ") greater than max_size (" + max_size + ") ")

    dest_file = open(destination, 'wb')
    dest_file.write(http_response.read())
    dest_file.close()

def read_first_line(file_path):
    infile = open(file_path, 'r')
    first_line = infile.readline()
    infile.close()
    return first_line

def file_contains(path, text):
    f = open(path, 'r')
    contents = f.read()
    f.close()
    return text in contents

def file_prepend(path, text):
    f = open(path)
    contents = f.read()
    f.close()

    f = open(path, 'w')
    f.write(text)
    f.write(contents)
    f.close()

def first_line_containing(value, infile):
    p1 = Popen(["grep", "-m", "1", "-n", value, infile], stdout=PIPE)
    p2 = Popen(["sed", "-n", 's/^\\([0-9]*\\)[:].*/\\1/p'], stdin=p1.stdout, stdout=PIPE)
    return int(p2.communicate()[0])

# Give group access to the specified path
def ensure_group_access(path):
    # Errs for any file not owned by this user.
    # But, the point is to set group writeability of any *new* files.
    execute('chmod -f -R g+rw %s' % path, halt_if_fail=False)

def set_umask():
    # umask g+rw
    os.umask(os.umask(0) & 0b001111)

def find_first_instance(regex, infile, delim=""):
    with open(infile, 'r') as f:
        pattern = re.compile(regex)
        for line in f:
            m = pattern.match(line)
            if m is not None:
                if pattern.groups > 0:
                    useDel = False
                    res = ""
                    for g in m.groups():
                        if useDel:
                            res = res + delim
                        else:
                            useDel = True
                        res = res + g
                    return res
                else:
                    return m.group(0)
    return None

def delete(file_to_delete):
    os.remove(file_to_delete)

def delete_if_exists(file_to_delete):
    if os.path.exists(file_to_delete):
        delete(file_to_delete)

def delete_path(path):
    ensure_group_access(path)
    shutil.rmtree(path)

def delete_path_if_exists(path):
    if os.path.exists(path):
        delete_path(path)

def prompt_or_auto_delete(path, auto):
    if not auto:
        prompt_to_delete(path)
    else:
        print
        delete_path_if_exists(path)

def is_yes(prompt_results):
    if prompt_results == "yes" or prompt_results == "Yes":
        return True
    return False

def is_no(prompt_results):
    if prompt_results == "no" or prompt_results == "No":
        return True
    return False


def prompt_to_delete(path):
    if os.path.exists(path):
        result = prompt_w_suggestion("Delete the following file:\n %s [Yes|No]" % path, "no", "^(Yes|yes|No|no)$")
        if result == "Yes" or result == "yes":
            delete_path(path)

def force_symlink(target_of_link, path_to_symlink):
    try:
        os.symlink(target_of_link, path_to_symlink)
    except OSError, e:
        if e.errno == errno.EEXIST:
            os.remove(path_to_symlink)
            os.symlink(target_of_link, path_to_symlink)

# note strs to find is mutated
def are_in_file(file_path, strs_to_find):
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
    dest_file = open(file_path, 'wb')
    dest_file.close()

#=========================================================================================
# Change Log utils

# Read every line in the file until we reach a line with at least 8 dashes("-") in it
def changelog_header(filename):
    f = open(filename, 'r')
    header = []

    for line in f:
        if '-------' in line:
            break
        header.append(line)

    return ''.join(header)

def get_changelog_date():
    return datetime.datetime.now().strftime("%d %b %Y")

# Ask the user whether they want to edit the changelog
# Prepend a changelog message to the changelog and then open the passed in editor
def edit_changelog(projectName, path, version, description, editor):
    edit = raw_input("Edit the %s changelog? [Y/n]" % projectName)
    if not edit == "n":
        if not file_contains(path, version):
            file_prepend(path, description)
            execute([editor, path])


def make_checkers_change_desc(version, changes):
    """Create a message to add to the current checker framework change log."""
    return """Version %s, %s


%s

----------------------------------------------------------------------
""" % (version, get_changelog_date(), changes)


def make_jsr308_change_desc(version, changes, latest_jdk):
    """Create a message to add to the current JSR308 change log."""
    return """Version %s, %s

Base build
  Updated to OpenJDK langtools build b%s

%s

----------------------------------------------------------------------
""" % (version, get_changelog_date(), latest_jdk, changes)

# Checker-Framework-specific changelog method.
# Queries whether or not the user wants to update the checker framework changelog
# then opens the changelog in the supplied editor
def edit_checkers_changelog(version, path, editor, changes=""):
    desc = make_checkers_change_desc(version, changes)
    edit_changelog("Checker Framework", path, version, desc, editor)


# JSR308 Specific Change log method
# Queries whether or not the user wants to update the checker framework changelog
# then opens the changelog in the supplied editor
def edit_langtools_changelog(version, openJdkReleaseSite, path, editor, changes=""):
    latest_jdk = latest_openjdk(openJdkReleaseSite)
    print "Latest OpenJDK release is b%s" % latest_jdk
    desc = make_jsr308_change_desc(version, changes, latest_jdk)
    edit_changelog("JSR308 Langtools", path, version, desc, editor)

#=========================================================================================
# Maven Utils

def mvn_deploy_file(name, binary, version, dest_repo, group_id, pom=None):
    pomOps = ""
    if pom is None:
        pomOps = "-DgeneratePom=True"
    else:
        pomOps = """-DgeneratePom=false -DpomFile=%s""" % (pom)

    command = """
    mvn deploy:deploy-file
        -DartifactId=%s
        -Dfile=%s
        -Dversion=%s
        -Durl=%s
        -DgroupId=%s
        -Dpackaging=jar
        """ % (name, binary, version, dest_repo, group_id) + pomOps
    return execute(command)

def mvn_deploy(binary, pom, url):
    command = """
    mvn deploy:deploy-file
        -Dfile=%s
        -DpomFile=%s
        -DgeneratePom=false
        -Durl=%s
        """ % (binary, pom, url)
    return execute(command)

def mvn_install(pluginDir):
    pom = pluginDirToPom(pluginDir)
    execute("mvn -f %s clean package" % pom)
    execute("mvn -f %s install" % pom)

def pluginDirToPom(pluginDir):
    return os.path.join(pluginDir, 'pom.xml')

def mvn_plugin_version(pluginDir):
    "Extract the version number from pom.xml in pluginDir/pom.xml"
    pomLoc = pluginDirToPom(pluginDir)
    dom = minidom.parse(pomLoc)
    version = ""
    project = dom.getElementsByTagName("project").item(0)
    for i in range(0, project.childNodes.length):
        childNode = project.childNodes.item(i)
        if childNode.nodeName == 'version':
            if childNode.hasChildNodes():
                version = childNode.firstChild.nodeValue
            else:
                print "Empty Maven plugin version!"
                sys.exit(1)
            break
    else:
        print "Could not find Maven plugin version!"
        sys.exit(1)
    return version

def find_mvn_plugin_jar(pluginDir, version, suffix=None):
    if suffix is None:
        name = "%s/target/checkerframework-maven-plugin-%s.jar" % (pluginDir, version)
    else:
        name = "%s/target/checkerframework-maven-plugin-%s-%s.jar" % (pluginDir, version, suffix)

    return name

def mvn_deploy_mvn_plugin(pluginDir, pom, version, mavenRepo):
    jarFile = find_mvn_plugin_jar(pluginDir, version)
    return mvn_deploy(jarFile, pom, mavenRepo)

def mvn_sign_and_deploy(url, repo_id, pom_file, file_property, classifier, pgp_user, pgp_passphrase):
    cmd = "mvn gpg:sign-and-deploy-file -Durl=%s -DrepositoryId=%s -DpomFile=%s -Dfile=%s" % (url, repo_id, pom_file, file_property)
    if classifier is not None:
        cmd += " -Dclassifier=" + classifier

    cmd += (" -Dgpg.keyname=%s '-Dgpg.passphrase=%s'" % (pgp_user, pgp_passphrase))

    execute(cmd)

def mvn_sign_and_deploy_all(url, repo_id, pom_file, artifact_jar, source_jar, javadoc_jar, pgp_user, pgp_passphrase):
    mvn_sign_and_deploy(url, repo_id, pom_file, artifact_jar, None, pgp_user, pgp_passphrase)
    mvn_sign_and_deploy(url, repo_id, pom_file, source_jar, "sources", pgp_user, pgp_passphrase)
    mvn_sign_and_deploy(url, repo_id, pom_file, javadoc_jar, "javadoc", pgp_user, pgp_passphrase)

#=========================================================================================
# Misc. Utils

def print_step(step):
    print  "\n"
    print  step

    dashStr = ""
    for dummy in range(0, len(step)):
        dashStr += "-"
    print  dashStr

#def find_project_locations():
#    afu_version       = max_version(AFU_INTERM_RELEASES_DIR)
#    jsr308_cf_version = max_version(JSR308_INTERM_RELEASES_DIR)
#
#    return {
#        LT_OPT : {
#            project_name : LT_OPT,
#
#            repo_source  : INTERM_JSR308_REPO,
#            repo_dest    : LIVE_JSR308_REPO,
#
#            deployment_source : os.path.join(JSR308_INTERM_RELEASES_DIR, jsr308_cf_version)
#            deployment_dest   : os.path.join(JSR308_LIVE_RELEASES_DIR, jsr308_cf_version)
#        },
#
#        AFU_OPT : {
#            project_name : AFU_OPT,
#
#            repo_source  : INTERM_AFU_REPO,
#            repo_dest    : LIVE_AFU_REPO,
#
#            deployment_source : os.path.join(AFU_INTERM_RELEASES_DIR, afu_version)
#            deployment_dest   : os.path.join(AFU_LIVE_RELEASES_DIR, afu_version)
#        },
#
#        CF_OPT : {
#            project_name : CF_OPT,
#
#            repo_source : INTERM_CHECKER_REPO,
#            repo_dest   : LIVE_CHECKER_REPO,
#
#            deployment_source : os.path.join(CHECKER_INTERM_RELEASES_DIR, jsr308_cf_version)
#            deployment_dest   : os.path.join(CHECKER_LIVE_RELEASES_DIR, jsr308_cf_version)
#        }
#    }
     #project_name, repo_source, repo_dest, deployment_source, deployment_dest

def get_announcement_email(version):
    return """
    To:  checker-framework-discuss@googlegroups.com
    Subject: Release %s of the Checker Framework

    We have released a new version of the Checker Framework and the Eclipse plugin for the Checker Framework.

    * The Checker Framework lets you create and/or run pluggable type checkers, in order to detect and prevent bugs in your code.
    * The Eclipse plugin makes it more convenient to run the Checker Framework.

    You can find documentation and download links for these projects at:
    http://CheckerFramework.org/

    Changes for the Checker Framework

    <<Insert latest Checker Framework changelog entry, omitting the first line with the release version and date, and with hard line breaks removed>>
    """ % (version)

#=========================================================================================
# Testing

def test_release_utils():
    test_max_version()
    test_increment_version()

# Tests run every time this file is loaded
test_release_utils()
