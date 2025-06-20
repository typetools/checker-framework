# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 11.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu
include(`Dockerfile-contents-ubuntu-base.txt')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-11-jdk \
&& update-java-alternatives -s java-1.11.0-openjdk-amd64

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
