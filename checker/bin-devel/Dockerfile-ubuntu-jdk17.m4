# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 17.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu
include(`Dockerfile-contents-ubuntu-base.m4')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  openjdk-17-jdk \
&& update-java-alternatives -s java-1.17.0-openjdk-amd64
ENV JAVA17_HOME=/usr/lib/jvm/java-17-openjdk-amd64

include(`Dockerfile-contents-apt-clean.m4')
