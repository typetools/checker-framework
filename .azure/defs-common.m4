changequote
changequote(`[',`]')dnl
ifelse([The built-in "dnl" m4 macro means "discard to next line",])dnl
define([canary_os], [ubuntu])dnl
define([canary_version], [25])dnl
define([latest_version], [25])dnl
define([canary_test], [canary_os[]canary_version])dnl
define([docker_testing], [])dnl
ifelse([uncomment the next line to use the "testing" Docker images])
ifelse([define([docker_testing], [-testing]])dnl
