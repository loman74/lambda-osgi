#!/bin/bash
set -x
####################################################
# deploy.sh
# Script for running provision
# FINRA , 2018
####################################################

RELEASE_DIR="${WORKSPACE}/release"
cd ${RELEASE_DIR}
export PROVISION_TAG=${PARENT_IMAGE_SUFFIX}
provision lambda $SDLC $COMMAND_ENV
