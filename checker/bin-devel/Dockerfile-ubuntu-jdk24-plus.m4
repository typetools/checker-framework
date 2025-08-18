# Create a Docker image that is ready to run the full Checker Framework tests,
# including building the manual and Javadoc, using JDK 24.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu:rolling
include(`Dockerfile-contents-ubuntu-base.m4')

include(`Dockerfile-contents-ubuntu-plus.m4')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-24-jdk \
&& update-java-alternatives -s java-1.24.0-openjdk-amd64
ENV JAVA24_HOME=/usr/lib/jvm/java-24-openjdk-amd64

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
