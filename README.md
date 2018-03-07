Groovy file use by Jenkins

1.master should add jdk and maven settting
jdk1.8  

/jdk1.8.0_45  

jdk1.7  

/jdk1.7.0_25

Maven-3.3.9

/maven-3.3.9

Maven-2.1.0

/maven-2.1.0

2.modify the sonar HTTP to http

3.volums to out the master and wingrow data(NFS)

4.docker registry update (supervisord.conf,wrapdocker,dockerupdate) 

/etc/supervisord.conf 

/usr/local/bin/dockerupdate.sh 

/usr/local/bin/wrapdocker


dockerindocker config

docker run -it --rm -v /var/run/docker.sock:/var/run/docker.sock -v /usr/bin/docker:/usr/bin/docker busybox /bin/bash
