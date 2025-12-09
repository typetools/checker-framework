# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 11.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu
include(`Dockerfile-contents-ubuntu-base.m4')dnl

# Install the JDK.
RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -qqy install \
  openjdk-11-jdk \
&& update-java-alternatives --set java-1.11.0-openjdk-amd64
ENV JAVA11_HOME=/usr/lib/jvm/java-11-openjdk-amd64

include(`Dockerfile-contents-apt-clean.m4')
