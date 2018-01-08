#!/bin/sh

#
# change the DOCKER_DAEMON_ARGS environment variable to /etc/supervisord.conf
#
sed -i -e "s/DOCKER_DAEMON_ARGS=.*/DOCKER_DAEMON_ARGS='$(echo $@| sed -e 's:/:\\/:g')'/" /etc/supervisord.conf

#
# update supervisor
#
supervisorctl update
