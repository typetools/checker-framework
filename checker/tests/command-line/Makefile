# All targets
.PHONY: all test clean skipped boxing-found-type issue618 override-message

# Tests that are currently passing
all: boxing-found-type issue618 override-message

test: all

clean:
	make -C boxing-found-type --quiet clean
	make -C issue618 --quiet clean
	make -C override-message --quiet clean

# Tests that are currently not passing
skipped:

boxing-found-type:
	make -C boxing-found-type

issue618:
	make -C issue618

override-message:
	make -C override-message
