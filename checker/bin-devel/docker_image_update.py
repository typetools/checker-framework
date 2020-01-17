#!/usr/bin/env python3

import argparse

upstream_dockerhub_username = "mdernst"
upstream_dockerhub_repository_prefix = "cf-ubuntu-"

dockerfile_prefix = "Dockerfile-ubuntu-for-cfi-"

opprop_dockerhub_username = "xingweitian"
opprop_dockerhub_repository_prefix = "ubuntu-for-cfi-"

# Fill correct information before executing the script file.
auth_config = {"username": "", "password": ""}


def parse_arguments():
    parser = argparse.ArgumentParser(
        description="Docker image update: Rebuild and push docker images automatically."
    )
    parser.add_argument(
        "names",
        type=str,
        nargs="+",
        help="a list of docker images' names waiting for being rebuilt.",
    )
    return parser.parse_args()


def rebuild_and_push_docker_images(name_list):
    try:
        import docker
    except ImportError:
        print("Please execute 'pip3 install docker --user' to install the dependency.")
        sys.exit(1)
    client = docker.from_env()
    for each_name in name_list:
        upstream_docker_repository_name = (
            upstream_dockerhub_username
            + "/"
            + upstream_dockerhub_repository_prefix
            + each_name
        )
        print("Now we are pulling: {}".format(upstream_docker_repository_name))
        client.images.pull(upstream_docker_repository_name, tag="latest")

        dockerfile_name = dockerfile_prefix + each_name
        print("Now we are rebuilding: {}".format(dockerfile_name))
        with open(dockerfile_name, "rb") as f:
            client.images.build(fileobj=f, tag="latest")

        opprop_docker_repository_name = (
            opprop_dockerhub_username
            + "/"
            + opprop_dockerhub_repository_prefix
            + each_name
        )
        print("Now we are pushing: {}".format(opprop_docker_repository_name))
        client.images.push(
            opprop_docker_repository_name, tag="latest", auth_config=auth_config
        )

        print(
            "Docker image: {} has been rebuilt and pushed.".format(
                opprop_docker_repository_name
            )
        )


if __name__ == "__main__":

    import sys

    if sys.version_info[0] == 3 and "" not in auth_config.values():
        name_list = parse_arguments().names
        rebuild_and_push_docker_images(name_list)
    else:
        print(
            "Please fill username and password for dockerhub before executing this script file.\n"
            "Also, Python3 is required. Try to run this script by using 'python3 docker_image_update.py'."
        )
