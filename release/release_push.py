#!/usr/bin/env python
# encoding: utf-8
"""
release_push.py

Created by Jonathan Burke on 2013-12-30.

Copyright (c) 2014 University of Washington. All rights reserved.
"""

from release_vars  import *
from release_utils import *
from sanity_checks import *
import urllib
import zipfile

#ensure that the latest built version is
def check_release_version( previous_release, new_release ):
    if version_to_integer( previous_release ) >= version_to_integer( new_release ):
        raise Exception( "Previous release version ( " + previous_release + " ) should be less than " +
                         "the new release version( " + new_release + " )" )

def copy_release_dir( path_to_dev, path_to_live, release_version ):
    source_location = os.path.join( path_to_dev, release_version )
    dest_location = os.path.join( path_to_live, release_version )

    if os.path.exists( dest_location ):
        prompt_to_delete( dest_location )

    if os.path.exists( dest_location ):
        raise Exception( "Destination location exists: " + dest_location )

    cmd = "cp -r %s %s" % ( source_location, dest_location )
    execute( cmd )

    return dest_location

def copy_releases_to_live_site( checker_version, afu_version):
    copy_release_dir( JSR308_INTERM_RELEASES_DIR,  JSR308_LIVE_RELEASES_DIR,  checker_version )
    copy_release_dir( CHECKER_INTERM_RELEASES_DIR, CHECKER_LIVE_RELEASES_DIR, checker_version )
    copy_release_dir( AFU_INTERM_RELEASES_DIR, AFU_LIVE_RELEASES_DIR, afu_version )

def update_release_symlinks( checker_version, afu_version ):
    afu_latest_release_dir = os.path.join( AFU_LIVE_RELEASES_DIR,  afu_version )
    checker_latest_release_dir = os.path.join( CHECKER_LIVE_RELEASES_DIR, checker_version )

    force_symlink( os.path.join( JSR308_LIVE_RELEASES_DIR,  checker_version ), os.path.join( JSR308_LIVE_SITE,  "current" ) )
    force_symlink( checker_latest_release_dir, os.path.join( CHECKER_LIVE_SITE, "current" ) )
    force_symlink( afu_latest_release_dir,     os.path.join( AFU_LIVE_SITE,     "current" ) )

    #After the copy operations the index.htmls will point into the dev directory
    force_symlink( os.path.join( afu_latest_release_dir,  "annotation-file-utilities.html" ), os.path.join( afu_latest_release_dir, "index.html" ) )
    force_symlink( os.path.join( checker_latest_release_dir, "checker-framework-webpage.html" ), os.path.join( checker_latest_release_dir, "index.html" ) )

def ensure_group_access_to_releases():
    ensure_group_access( JSR308_LIVE_RELEASES_DIR )
    ensure_group_access( AFU_LIVE_RELEASES_DIR )
    ensure_group_access( CHECKER_LIVE_RELEASES_DIR )

def push_maven_artifacts_to_release_repo( version ):
    mvn_deploy_mvn_plugin( MAVEN_PLUGIN_DIR, MAVEN_PLUGIN_POM, version, MAVEN_LIVE_REPO )

    # Deploy jsr308 and checker-qual jars to maven repo
    mvn_deploy( CHECKER_BINARY, CHECKER_BINARY_POM, MAVEN_LIVE_REPO )
    mvn_deploy( CHECKER_QUAL,   CHECKER_QUAL_POM,   MAVEN_LIVE_REPO )
    mvn_deploy( JAVAC_BINARY,   JAVAC_BINARY_POM,   MAVEN_LIVE_REPO )
    mvn_deploy( JDK7_BINARY,    JDK7_BINARY_POM,    MAVEN_LIVE_REPO )
    mvn_deploy( JDK8_BINARY,    JDK8_BINARY_POM,    MAVEN_LIVE_REPO )

def stage_maven_artifacts_in_maven_central( new_checker_version ):

    pgp_user="checker-framework-dev@googlegroups.com"
    pgp_passphrase = read_first_line( PGP_PASSPHRASE_FILE )

    mvn_dist = os.path.join(MAVEN_PLUGIN_DIR, "dist" )
    execute( "mkdir -p " + mvn_dist )

    #build Jar files with only readmes for artifacts that don't have sources/javadocs
    ant_cmd = "ant -f release.xml -Ddest.dir=%s -Dmaven.plugin.dir=%s jar-maven-extras" % (mvn_dist, MAVEN_PLUGIN_DIR)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #At the moment, checker.jar is the only artifact with legitimate accompanying source/javadoc jars
    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_BINARY_RELEASE_POM, CHECKER_BINARY,
                             CHECKER_SOURCE, CHECKER_JAVADOC,
                             pgp_user, pgp_passphrase )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_QUAL_RELEASE_POM, CHECKER_QUAL,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_COMPAT_QUAL_RELEASE_POM,
                             CHECKER_COMPAT_QUAL,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-compat-qual-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-compat-qual-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JAVAC_BINARY_RELEASE_POM, JAVAC_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK7_BINARY_RELEASE_POM, JDK7_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK8_BINARY_RELEASE_POM, JDK8_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-javadoc.jar" ),
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JAVACUTIL_BINARY_RELEASE_POM, JAVACUTIL_BINARY,
                             JAVACUTIL_SOURCE_JAR, JAVACUTIL_JAVADOC_JAR,
                             pgp_user, pgp_passphrase  )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, DATAFLOW_BINARY_RELEASE_POM, DATAFLOW_BINARY,
                             DATAFLOW_SOURCE_JAR, DATAFLOW_JAVADOC_JAR,
                             pgp_user, pgp_passphrase  )

    plugin_jar = find_mvn_plugin_jar( MAVEN_PLUGIN_DIR, new_checker_version )
    plugin_source_jar  = find_mvn_plugin_jar( MAVEN_PLUGIN_DIR, new_checker_version, "sources" )
    plugin_javadoc_jar = os.path.join( MAVEN_RELEASE_DIR, mvn_dist, "checkerframework-maven-plugin-javadoc.jar" )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, MAVEN_PLUGIN_RELEASE_POM, plugin_jar,
                             plugin_source_jar, plugin_javadoc_jar, pgp_user, pgp_passphrase  )

    delete_path( mvn_dist )

def run_link_checker( site, output ):
    check_links_script = os.path.join(CHECKER_FRAMEWORK_RELEASE, "checkLinks.sh")
    cmd = ["sh", check_links_script, site]

    out_file = open( output, 'w+' )

    print("Executing: " + " ".join(cmd) )
    process = subprocess.Popen(cmd, stdout=out_file, stderr=out_file)
    process.communicate()
    process.wait()
    out_file.close()

    if process.returncode != 0:
        raise Exception('Non-zero return code( %s ) while executing %s' % (process.returncode, cmd))

    return output

def check_all_links( jsr308_website, afu_website, checker_website, suffix ):
    jsr308Check  = run_link_checker( jsr308_website,  "/tmp/jsr308." + suffix + ".check" )
    afuCheck     = run_link_checker( afu_website,     "/tmp/afu." + suffix + ".check" )
    checkerCheck = run_link_checker( checker_website, "/tmp/checker-framework." + suffix + ".check" )
    print( "Link checker results can be found at:\n" +
           "\t" + jsr308Check  + "\n" +
           "\t" + afuCheck     + "\n" +
           "\t" + checkerCheck + "\n" )

    continue_script = prompt_w_suggestion("Delete DEV site link checker results?", "yes", "^(Yes|yes|No|no)$")
    if is_yes(continue_script):
        delete( jsr308Check )
        delete( afuCheck )
        delete( checkerCheck )

def push_interm_to_release_repos():
    hg_push_or_fail( INTERM_JSR308_REPO )
    hg_push_or_fail( INTERM_ANNO_REPO )
    hg_push_or_fail( INTERM_CHECKER_REPO )

def continue_or_exit( msg ):
    continue_script = prompt_w_suggestion(msg + " Continue?", "yes", "^(Yes|yes|No|no)$")
    if continue_script == "no" or continue_script == "No":
                raise Exception( "User elected NOT to continue at prompt: " + msg )

def read_args(argv):
    test = True
    if len( argv ) == 2:
        if argv[1] == "release":
            test = False
        else:
            print_usage()
    else:
        if len( argv ) > 2:
            print_usage()
            raise Exception( "Invalid arguments. " + ",".join(argv) )

    return test

def print_usage():
    print ( "Usage: python release_build.py [release]\n" +
            "The only argument this script takes is \"release\".  If this argument is " +
            "NOT specified then the script will execute all steps that checking and prompting " +
            "steps but will NOT actually perform a release.  This is for testing the script." )

def main(argv):
    set_umask()

    test_mode = read_args( argv )

    msg = ( "You have chosen test_mode.  \nThis means that this script will execute all build steps that " +
            "do not have side-effects.  That is, this is a test run of the script.  All checks and user prompts "  +
            "will be shown but no steps will be executed that will cause the release to be deployed or partially " +
            "deployed.\n" +
            "If you meant to do an actual release, re-run this script with one argument, \"release\"." )

    if not test_mode:
        msg = "You have chosen release_mode.  Please follow the prompts to run a full Checker Framework release"

    continue_or_exit( msg + "\n" )
    if test_mode:
        print("Continuing in test mode.")
    else:
        print("Continuing in release mode.")

    check_hg_user()

    print_step( "Push Step 0: Verify Requirements\n" )
    print( " If this is your first time running the release_push script, please verify that you have met " +
           "all the requirements specified in README-maintainers.html \"Pre-release Checklist\"\n" )

    continue_or_exit("")

    print_step( "Push Step 1: Checking release versions" )
    dev_jsr308_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "jsr308" )
    live_jsr308_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "jsr308" )
    dev_afu_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "annotation-file-utilities" )
    live_afu_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "annotation-file-utilities" )

    dev_checker_website  = os.path.join( HTTP_PATH_TO_DEV_SITE,  "checker-framework" )
    live_checker_website = os.path.join( HTTP_PATH_TO_LIVE_SITE, "checker-framework" )
    current_checker_version = current_distribution_by_website( live_checker_website )
    new_checker_version     = current_distribution( CHECKER_FRAMEWORK )
    check_release_version( current_checker_version, new_checker_version )

    #note, get_afu_version_from_html uses the file path not the web url
    dev_afu_website_file  = os.path.join( FILE_PATH_TO_DEV_SITE,  "annotation-file-utilities", "index.html" )
    live_afu_website_file = os.path.join( FILE_PATH_TO_LIVE_SITE, "annotation-file-utilities", "index.html" )
    current_afu_version = get_afu_version_from_html( live_afu_website_file )
    new_afu_version     = get_afu_version_from_html( dev_afu_website_file )
    check_release_version( current_afu_version, new_afu_version )

    print("Checker Framework/JSR308:  current-version=%s    new-version=%s" % (current_checker_version, new_checker_version ) )
    print("AFU:                       current-version=%s    new-version=%s" % (current_afu_version, new_afu_version ) )

    continue_or_exit("Please ensure that you have run release_build.py since the last push to " +
                     "any of the JSR308, AFU, or Checker Framework repositories." )

    print_step( "Push Step 2: Check links on development site" )

    # Work around broken link to dejavu.css by creating an empty dejavu.css file
    dev_afu_website_api_directory  = os.path.join( FILE_PATH_TO_DEV_SITE, "checker-framework", "api" )
    execute("mkdir -p resources/fonts", True, False, dev_afu_website_api_directory)
    execute("touch resources/fonts/dejavu.css", True, False, dev_afu_website_api_directory)

    if prompt_yes_no( "Run link Checker on DEV site?", True ):
        check_all_links( dev_jsr308_website, dev_afu_website, dev_checker_website, "dev" )

    print_step( "Push Step 3: Run development sanity tests" )
    continue_or_exit(
       "Later in this step you will build and install the Eclipse plugin using the latest artifacts. See\n" +
       "README-developers.html under the checker-framework/release directory\n\n")

    print_step(" 3a: Run javac sanity test on development release." )
    if prompt_yes_no( "Run javac sanity test on development release?", True ):
        javac_sanity_check( dev_checker_website, new_checker_version )

    print_step("3b: Run Maven sanity test on development release." )
    if prompt_yes_no( "Run Maven sanity test on development repo?", True ):
        maven_sanity_check( "maven-dev", MAVEN_DEV_REPO, new_checker_version )

    print_step( "3c: Build the Eclipse plugin and test." )
    print("Please download: http://types.cs.washington.edu/dev/checker-framework/current/checker-framework.zip")
    print("Use the jars in the dist directory along with the instructions at " +
          "checker-framework/eclipse/developer-README to build the Eclipse plugin to build the Eclipse plugin." +
          "Please install this version in the latest version of Eclipse and follow the tutorial at:\n" +
          "http://types.cs.washington.edu/dev/checker-framework/tutorial/" )
    continue_or_exit("If the tutorial doesn't work, please abort the release and contact the appropriate developer.")

    print_step( "Push Step 4: Stage Maven artifacts in Central" )

    print_step("4a: Stage the artifacts at Maven central." )
    if prompt_yes_no( "Stage Maven artifacts in Maven Central?" ):
        stage_maven_artifacts_in_maven_central( new_checker_version )

        print_step("4b: Close staged artifacts at Maven central." )
        print( "Maven artifacts have been staged!  Please 'close' (but don't release) the artifacts. " +
               "To close, log into https://oss.sonatype.org using your " +
               "Sonatype credentials and follow the 'close' instructions at: " + SONATYPE_CLOSING_DIRECTIONS_URL )

        print_step("4c: Run Maven sanity test on Maven central artifacts." )
        if prompt_yes_no( "Run Maven sanity test on Maven central artifacts?", True ):
            repo_url = raw_input( "Please enter the repo URL of the closed artifacts.  To find this URL " +
                                  "log into https://oss.sonatype.org.  Go to the Staging Repositories.  Find " +
                                  "the repository you just closed and paste that URL here:\n" )

            maven_sanity_check( "maven-staging", repo_url, new_checker_version )

    print_step("Push Step 5. Push dev website to live website" )
    continue_or_exit("Copy release to the live website?")
    if not test_mode:
        print("Copying to live site")
        copy_releases_to_live_site( new_checker_version, new_afu_version )
        ensure_group_access_to_releases()
        update_release_symlinks( new_checker_version, new_afu_version )
    else:
        print( "Test mode: Skipping copy to live site!" )

    print( "Push Step 6. Check live site links" )
    if prompt_yes_no( "Run link Checker on LIVE site?", True ):
        check_all_links( live_jsr308_website, live_afu_website, live_checker_website, "live" )

    print_step( "Push Step 7: Run javac sanity test on the live release." )
    if prompt_yes_no( "Run javac sanity test on live release?", True ):
        javac_sanity_check( live_checker_website, new_checker_version )

    print_step("Push Step 8: Deploy the Eclipse Plugin to the live site." )
    continue_or_exit( "Follow the instruction under 'UW Website' in checker-framework/eclipse/developer-README to " +
                      "deploy the Eclipse plugin to the live website.  Please install the plugin from the new " +
                      "live repository and run it on a file in which you expect a type error.  If you run into errors, " +
                      "back out the release!" )

    print_step( "Push Step 9. Commit changes to repositories" )
    if prompt_yes_no( "Push the release to Google code repositories?  This is irreversible." ):
        if not test_mode:
            push_interm_to_release_repos()
            print( "Pushed to repos" )
        else:
            print( "Test mode: Skipping push to Google Code!" )

    print_step( "Push Step 10. Release staged artifacts in Central repository." )
    if test_mode:
        msg = ( "Test Mode: You are in test_mode.  Please 'DROP' the artifacts. "   +
                "To drop, log into https://oss.sonatype.org using your " +
                "Sonatype credentials and follow the 'DROP' instructions at: " + SONATYPE_DROPPING_DIRECTIONS_URL )
    else:
        msg = ( "Please 'release' the artifacts, but first ensure that the Checker Framework maven plug-in directory" +
                "(and only that directory) is removed from the artifacts." +
                "To release, log into https://oss.sonatype.org using your " +
                "Sonatype credentials and follow the 'close' instructions at: " + SONATYPE_RELEASE_DIRECTIONS_URL )

    print( msg )
    prompt_until_yes()

    print_step( "Push Step 11. Update pushed issues." )
    continue_or_exit( "Please log in to google code and mark all issues that were 'pushed' to 'fixed' (as well all issues mentioned as fixed in the changelogs)." )

    print_step( "Push Step 12. Announce the release." )
    continue_or_exit( "Please announce the release using the email structure below:\n" +
                       get_announcement_email( new_checker_version ) )

    print_step( "Push Step 13. Push Eclipse plugin files." )
    if test_mode:
        msg = ( "Test Mode: You are in test_mode.  If you built the Eclipse plugin on"   +
                "your local machine, you may want to revert any files that were modified." )
    else:
        msg = ( "If you built the Eclipse plugin on your local machine, there are a few" +
                "changed files with version number changes that need to be pushed.\n" +
                "Do not push the .classpath file. The following files should be pushed:\n" +
                "checker-framework-eclipse-feature/feature.xml\n" +
                "checker-framework-eclipse-plugin/META-INF/MANIFEST.MF\n" +
                "checker-framework-eclipse-update-site/site.xml" )

    print( msg )
    prompt_until_yes()

if __name__ == "__main__":
    sys.exit(main(sys.argv))
