# Create a Docker image that is ready to run the main Checker Framework tests,
# using JDK 25.

# "ubuntu" is the latest LTS release.  "ubuntu:rolling" is the latest release.
# Both might lag behind; as of 2024-11-16, ubuntu:rolling was still 24.04 rather than 24.10.
FROM ubuntu:rolling
include(`Dockerfile-contents-ubuntu-base.txt')

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt -qqy update \
&& apt -y install \
  openjdk-25-jdk \
&& update-java-alternatives -s java-1.25.0-openjdk-amd64
ENV JAVA25_HOME=/usr/lib/jvm/java-25-openjdk-amd64

RUN export DEBIAN_FRONTEND=noninteractive \
&& apt autoremove \
&& apt clean \
&& rm -rf /var/lib/apt/lists/*
