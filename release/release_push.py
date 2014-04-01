#!/usr/bin/env python
# encoding: utf-8
"""
release_push.py

Created by Jonathan Burke on 2013-12-30.

Copyright (c) 2013 University of Washington. All rights reserved.
"""

from release_vars  import *
from release_utils import *
import urllib
import zipfile

#ensure that the latest built version is
def check_release_version( previous_release, new_release ):
    if version_to_integer( previous_release ) >= version_to_integer( new_release ):
        raise Exception( "Previous release version ( " + previous_release + " ) should be less than " +
                         "the new release version( " + new_release + " )" )

def run_sanity_check( new_checkers_release_zip, release_version ):
    execute( "mkdir -p " + SANITY_DIR )

    sanity_zip = os.path.join( SANITY_DIR, "checker-framework.zip" )

    print( "Attempting to download %s to %s" % ( new_checkers_release_zip, sanity_zip ) )
    download_binary( new_checkers_release_zip, sanity_zip, MAX_DOWNLOAD_SIZE )

    deploy_dir = os.path.join( SANITY_DIR, "checker-framework-" + release_version )

    if os.path.exists( deploy_dir ):
        print( "Deleting existing path: " + deploy_dir )
        delete_path(deploy_dir)

    with zipfile.ZipFile(sanity_zip, "r") as z:
        z.extractall( SANITY_DIR )

    cmd = "chmod -R u+rwx " + deploy_dir
    execute( cmd )

    sanity_javac = os.path.join( deploy_dir, "binary", "javac" )
    nullness_example = os.path.join( deploy_dir, "examples", "NullnessExampleWithWarnings.java" )
    nullness_output  = os.path.join( deploy_dir, "output.log" )

    cmd = sanity_javac + " -processor org.checkerframework.checker.nullness.NullnessChecker " + nullness_example + " -Anomsgtext &> " + nullness_output
    execute( cmd, False )

    expected_errors = [ "NullnessExampleWithWarnings.java:25: error: (assignment.type.incompatible)",
                        "NullnessExampleWithWarnings.java:36: error: (argument.type.incompatible)"  ]
    found_errors = are_in_file( nullness_output, expected_errors )

    if not found_errors:
        raise Exception( "Sanity check did not work!\n" +
                         "File: " + nullness_output + "\n" +
                         "should contain the following errors: [ " + ", ".join(expected_errors) )
    else:
        print( "\nsanity check - zip file: passed!\n" )

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
    force_symlink( os.path.join( JSR308_LIVE_RELEASES_DIR,  checker_version ), os.path.join( JSR308_LIVE_SITE,  "current" ) )
    force_symlink( os.path.join( CHECKER_LIVE_RELEASES_DIR, checker_version ), os.path.join( CHECKER_LIVE_SITE, "current" ) )
    force_symlink( os.path.join( AFU_LIVE_RELEASES_DIR,     afu_version ),     os.path.join( AFU_LIVE_SITE,     "current" ) )

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

def stage_maven_artifacts_in_maven_central():
    mvn_dist = os.path.join(MAVEN_PLUGIN_DIR, "dist" )
    execute( "mkdir -p " + mvn_dist )

    #build Jar files with only readmes for artifacts that don't have sources/javadocs
    ant_cmd = "ant -f release.xml -Ddist.dir=%s -Dmaven.plugin.dir=%s" % (mvn_dist, MAVEN_PLUGIN_DIR)
    execute(ant_cmd, True, False, CHECKER_FRAMEWORK_RELEASE)

    #At the moment, checker.jar is the only artifact with legitimate accompanying source/javadoc jars
    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_BINARY_RELEASE_POM, CHECKER_BINARY,
                             CHECKER_SOURCE, CHECKER_JAVADOC )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, CHECKER_QUAL_RELEASE_POM, CHECKER_QUAL,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "checker-qual-javadoc.jar" ) )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JAVAC_RELEASE_POM, JAVAC_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "compiler-javadoc.jar" ) )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK7_RELEASE_POM, JDK7_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk7-javadoc.jar" ) )

    mvn_sign_and_deploy_all( SONATYPE_OSS_URL, SONATYPE_STAGING_REPO_ID, JDK8_RELEASE_POM, JDK8_BINARY,
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-source.jar"  ),
                             os.path.join(MAVEN_RELEASE_DIR, mvn_dist, "jdk8-javadoc.jar" ) )

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

def main(argv):
    stage_maven_artifacts_in_maven_central()

def main(argv):
    print(
        "\nVerify Sonatype Credentials\n" +
          "---------------------------\n" +
        "In order to stage artifacts to the Sonatype Central repositories "   +
        "you will need an account at:\n"        +
        "https://issues.sonatype.org/\n\n"       +

        "You will also need to setup your Maven settings.xml to include your " +
        "Sonatype credentials as outline in: \n" +
        "https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-7b.StageExistingArtifacts\n\n" +
        "Finally, section 2 in the above link will also describe how to be added to " +
        "an existing repository's committers list.\n\n" )

    continue_or_exit("")


    print( "\n\nChecking Release Versions\n" +
                "-------------------------\n" )
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

    new_checkers_release_zip = os.path.join( dev_checker_website, "current", "checker-framework.zip" )

    print("Checker Framework/JSR308:  current-version=%s    new-version=%s\n" % (current_checker_version, new_checker_version ) )
    print("AFU:                       current-version=%s    new-version=%s\n" % (current_afu_version, new_afu_version ) )

    continue_or_exit("Please ensure that you have run release_build.py since the last push to " +
                     "any of the JSR308, AFU, or Checker Framework repositories." )


    print( "\n\nCheck Dev Site Links\n" +
               "--------------------\n" )
    continue_script = prompt_w_suggestion("Run link Checker on DEV site?", "yes", "^(Yes|yes|No|no)$")
    if is_yes( continue_script ):
        check_all_links( dev_jsr308_website, dev_afu_website, dev_checker_website, "dev" )

    print( "\n\nSmoke Tests\n" +
               "-----------\n" )
    continue_or_exit(
       "Please build and install the Eclipse plugin using the latest artifacts. See:\n" +
       "https://code.google.com/p/checker-framework/source/browse/eclipse/developer-README\n\n" +

       "Please run the Eclipse version of  the Checker Framework Tutorial. See:\n"      +
       dev_checker_website + "\n\n" +

       "Note: You will be prompted to run the Maven tutorial (automatically, via this script) below.\n\n" )

    run_sanity = prompt_w_suggestion(" Run command-line sanity check?", "yes", "^(Yes|yes|No|no)$")
    if is_yes( run_sanity ):
        run_sanity_check( new_checkers_release_zip, new_checker_version )

    #Run Maven tutorial
    print( "Please run the Maven examples\n" )

    print( "\n\nPush Website\n" +
               "------------\n" )
    continue_or_exit("Copy release to the live website?")
    copy_releases_to_live_site( new_checker_version, new_afu_version )
    ensure_group_access_to_releases()
    update_release_symlinks( new_checker_version, new_afu_version )

    print( "\n\nCheck Dev Site Links\n" +
               "--------------------\n" )
    continue_script = prompt_w_suggestion("Run link Checker on LIVE site?", "yes", "^(Yes|yes|No|no)$")
    if is_yes( continue_script ):
        check_all_links( live_jsr308_website, live_afu_website, live_checker_website, "live" )

    print( "\n\nPush Maven Artifacts\n" +
               "--------------------\n" )
    continue_script = prompt_w_suggestion("Push Maven artifacts to our repository?", "no", "^(Yes|yes|No|no)$")
    if continue_script == "yes" or continue_script == "Yes":
        push_maven_artifacts_to_release_repo( new_checker_version )
        print( "Pushed Maven Artifacts to repos" )

    continue_script = prompt_w_suggestion("Stage Maven artifacts in Maven Central?", "no", "^(Yes|yes|No|no)$")
    if continue_script == "yes" or continue_script == "Yes":
        #stage_maven_artifacts_in_maven_central( new_checker_version )
        print( "Maven artifacts have been staged!  To deploy, log into https://oss.sonatype.org using your " +
               "Sonatype credentials and follow the release instructions at: " + SONATYPE_RELEASE_DIRECTIONS_URL )

    print( "\n\nCommit to Repositories\n" +
               "----------------------\n" )
    continue_script = prompt_w_suggestion("Push the release to Google code repositories?  This is irreversible.", "no", "^(Yes|yes|No|no)$")
    if continue_script == "yes" or continue_script == "Yes":
        push_interm_to_release_repos()
        print( "Pushed to repos" )

    continue_or_exit( "Please follow these instructions to release the Eclipse plugin:\n<path to Eclipse release instructions>\n" )
    continue_or_exit( "Please log in to google code and mark all issues that were 'pushed' to 'fixed'." )
    continue_or_exit( "Please announce the release using the email structure below:\n" +
                       get_announcement_email( new_checker_version ) )

if __name__ == "__main__":
    sys.exit(main(sys.argv))