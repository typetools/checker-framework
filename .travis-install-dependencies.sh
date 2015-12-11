#!/bin/bash

# Install jtreg; currently must be from source.
(cd .. && wget https://adopt-openjdk.ci.cloudbees.com/view/OpenJDK/job/jtreg/lastSuccessfulBuild/artifact/jtreg-4.1-b12.tar.gz && tar xzf jtreg-4.1-b12.tar.gz)
