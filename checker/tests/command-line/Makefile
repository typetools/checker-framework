# All targets
.PHONY: all test clean skipped issue618 override-message

# Tests that are currently passing
all: issue618 override-message

test: all

clean:
	make -C issue618 --quiet clean
	make -C override-message --quiet clean

# Tests that are currently not passing
skipped:

issue618:
	make -C issue618

override-message:
	make -C override-message
