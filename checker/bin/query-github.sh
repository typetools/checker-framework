#!/bin/sh

# This script collects a list of projects that match a query from GitHub.

# inputs:
#
# The file git-personal-access-token must exist in the directory from which
# this script is run, and must be a valid github OAuth token.  The token only
# needs the "Access public repositories" permission.
#
# $1 is the query file, which should contain the literal string to use
# as the github search. REQUIRED, no default.
#
# $2 is the number of GitHub search pages.  Default 1. Each page contains 10 results.  GitHub only
# returns the first 1000 results, so 100 is the maximum useful number of search pages.
#
# This script outputs a list of projects. The underlying GitHub search is for code snippets, and this
# script eliminates duplicates (i.e. different code snippets from the same project are combined into
# a single result for the project), so the number of projects the script outputs will usually be less
# than 10 times the number of pages requested.

# Set to 1 to enable debug output from this script.
DEBUG=0

query_file=$1
# Number of times to retry a GitHub search query.
query_tries=5

if [ -z "${query_file}" ]; then
    echo "you must provide a query file as the first argument"
    exit 2
fi

if [ -z "$2" ]; then
    page_count=1
else
    page_count=$2
fi

query=$(tr ' ' '+' < "${query_file}")

mkdir -p "/tmp/$USER"

## for storing the results before sorting and uniqing them
rm -f "/tmp/$USER/github-query-results-*.txt"
tempfile=$(mktemp "/tmp/$USER/github-query-results-$(date +%Y%m%d-%H%M%S)-XXX.txt")
#trap "rm -f ${tempfile}" 0 2 3 15

rm -f "/tmp/$USER/github-hash-results-*.txt"
hashfile=$(mktemp "/tmp/$USER/github-hash-results-$(date +%Y%m%d-%H%M%S)-XXX.txt")
#trap "rm -f ${hashfile}" 0 2 3 15

rm -rf "/tmp/$USER/curl-output-*.txt"
curl_output_file=$(mktemp "/tmp/$USER/curl-output-$(date +%Y%m%d-%H%M%S)-XXX.txt")

# find the repos
for i in $(seq "${page_count}"); do
    # GitHub only allows 30 searches per minute, so add a delay to each request.
    if [ "${i}" -gt 1 ]; then
        sleep 5
    fi

    full_query='https://api.github.com/search/code?q='${query}'&page='${i}
    if [ $DEBUG -ne 0 ] ; then
        echo "full_query=$full_query"
    fi
    for tries in $(seq ${query_tries}); do
        status_code=$(curl -s \
            -H "Authorization: token $(cat git-personal-access-token)" \
            -H "Accept: application/vnd.github.v3+json" \
            -w "%{http_code}" \
            -o "${curl_output_file}" \
            "${full_query}")

        if [ "${status_code}" -eq 200 ] || [ "${status_code}" -eq 422 ]; then
            # Don't retry.
            # 200 is success.  422 means too many GitHub requests.
            break
        elif [ "${tries}" -lt $((query_tries - 1)) ]; then
            # Retry.
            # Other status codes are failures. Failures are usually due to
            # triggering the abuse detection mechanism for sending too many
            # requests, so we add a delay when this happens.
            sleep 20
        fi
    done

    # GitHub only returns the first 1000 results. Requests past this limit
    # return 422, so stop making requests.
    if [ "${status_code}" -eq 422 ]; then
        break;
    elif [ "${status_code}" -ne 200 ]; then
        echo "GitHub query failed, last response:"
        cat "${curl_output_file}"
        rm -f "${curl_output_file}"
        exit 1
    fi

    grep "        \"html_url" < "${curl_output_file}" \
        | grep -v "          " \
        | sort -u \
        | cut -d \" -f 4 >> "${tempfile}"
done

rm -f "${curl_output_file}"

# Each loop iteration was sorted and unique; this does it for the full result.
sort -u -o "${tempfile}" "${tempfile}"

while IFS= read -r line
do
    repo=$(echo "${line}" | cut -d / -f 5)
    owner=$(echo "${line}" | cut -d / -f 4)
    hash_query='https://api.github.com/repos/'${owner}'/'${repo}'/commits?per_page=1'
    curl -sH "Authorization: token $(cat git-personal-access-token)" \
             "Accept: application/vnd.github.v3+json" \
             "${hash_query}" \
        | grep '^    "sha":' \
        | cut -d \" -f 4 >> "${hashfile}"
done < "${tempfile}"

paste "${tempfile}" "${hashfile}"
