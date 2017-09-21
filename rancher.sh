#!/bin/bash
set -x 
function restart_service() {
    local environment=$1
    local service=$2
    local batchSize=$3
    local interval=$4
    
    curl -u "${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}" \
        -X POST \
        -H 'Accept: application/json' \
        -H 'Content-Type: application/json' \
        -d '{"rollingRestartStrategy": {"batchSize": '${batchSize}', "intervalMillis": '${interval}'}}' \
        "${CATTLE_URL}/projects/${environment}/services/${service}?action=restart"
}
function upgrade_service() {
    local environment=$1
    local service=$2
    local image=$3
    
    local inServiceStrategy=`curl -u "${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}" \
        -X GET \
        -H 'Accept: application/json' \
        -H 'Content-Type: application/json' \
        "${CATTLE_URL}/projects/${environment}/services/${service}/" | jq '.upgrade.inServiceStrategy'`
    local updatedServiceStrategy=`echo ${inServiceStrategy} | jq ".launchConfig.imageUuid=\"docker:${image}\""`
    echo "updatedServiceStrategy "${updatedServiceStrategy}
    echo "sending update request"
 
        
    curl -u "${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}" \
        -X POST \
        -H 'Accept: application/json' \
        -H 'Content-Type: application/json' \
        -d "{
          \"inServiceStrategy\": ${updatedServiceStrategy}
          }
        }" \
        "${CATTLE_URL}/projects/${environment}/services/${service}?action=upgrade"
}
function finish_upgrade() {
    local environment=$1
    local service=$2
    echo "waiting for service to upgrade "
    let i=0
    # waiting 10 minus
    while [ $i -lt 30 ]; do
     
      i=$(( $i + 1))
      echo "i="$i 
       sleep 20
      local serviceState=`curl -u "${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}" \
          -X GET \
          -H 'Accept: application/json' \
          -H 'Content-Type: application/json' \
          "${CATTLE_URL}/projects/${environment}/services/${service}/" | jq '.state'`
      case $serviceState in
          "\"upgraded\"" )
              echo "completing service upgrade"
              curl -u "${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}" \
                -X POST \
                -H 'Accept: application/json' \
                -H 'Content-Type: application/json' \
                -d '{}' \
                "${CATTLE_URL}/projects/${environment}/services/${service}?action=finishupgrade"
              break ;;
          "\"upgrading\"" )
              echo -n "."
              sleep 3
              continue ;;
          *)
            echo "unexpected upgrade state: $serviceState" ;exit 1;
              
      esac
    done
}

if [[ -n "${DeployToRancher_environment}" ]]; then
    RANCHER_ENV=${DeployToRancher_environment}
else
    RANCHER_ENV=`cat ~/.rancher/cli.json |jq .environment|sed 's/"//g' ` 
fi

CATTLE_SECRET_KEY=`cat ~/.rancher/cli.json |jq .secretKey|sed 's/"//g' ` 
CATTLE_ACCESS_KEY=`cat ~/.rancher/cli.json |jq .accessKey|sed 's/"//g' ` 
CATTLE_URL=`cat ~/.rancher/cli.json |jq .url|awk -F "//" '{print "http://"$2"/v2-beta"}' `  
#${DeployToRancher_arg}  '${DeployToRancher_service}' '${imagename}' ${DeployToRancher_cmd} 
#export DeployToRancher_service=$2
#export imagename=$3
#export DeployToRancher_arg=$1
#export DeployToRancher_cmd=$4
serviceId=`rancher --env ${RANCHER_ENV} ps --format '{{.Stack.Name}}/{{.Service.Name}} {{.Service.Id}}'| grep ${DeployToRancher_service} | awk {'print $2'}`
 
if [ $serviceId ]    # string1 has not been declared or initialized.
then
     upgrade_service ${RANCHER_ENV} ${serviceId} ${imagename}
   sleep 3
   finish_upgrade  ${RANCHER_ENV}  ${serviceId}
else  

    rancher --env ${RANCHER_ENV} run ${DeployToRancher_arg} --name ${DeployToRancher_service} \
      ${imagename} ${DeployToRancher_cmd}
fi        
