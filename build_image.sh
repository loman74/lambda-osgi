#!/bin/bash
#build_image.sh
# ##############################################################################
# Script would be executed by Jenkins CREATE_IMAGES Stage
#
# FINRA , 2018
###############################################################################

set -ex pipefail

cd ${WORKSPACE}/release

sudo $(aws ecr get-login --region us-east-1 --no-include-email --registry-ids 465257512377)

PARENT_COMPONENT=crisp-lambda-java

# crisp-dm-client
mkdir -p -m775 ./crisp-dm-client
aws s3 cp s3://4652-5751-2377-application-dev-staging/CRISP/crisp-dm-client/crisp-dm-client-0.0.1-SNAPSHOT-DM.jar ./crisp-dm-client/crisp-dm-client-0.0.1-SNAPSHOT-DM.jar
chmod -R 755 ./crisp-dm-client

sudo docker build -t ${PARENT_COMPONENT} . --build-arg	APRO_USER="${APRO_USER}" --build-arg APRO_PASSWORD="${APRO_PASSWORD}"
sudo docker save ${PARENT_COMPONENT} > ${PARENT_COMPONENT}.tar






