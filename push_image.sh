#!/bin/bash

set -x

####################################################
# push_image.sh
# Script for pushing images
# FINRA , 2018
####################################################
export IMAGE_VERSION=1.0.0
export PARENT_COMPONENT=crisp-lambda-java
export AWS_ACCOUNT=`python -c "import boto.utils; print boto.utils.get_instance_metadata()['iam']['info']['InstanceProfileArn'].split(':')[4]"`
export IMAGE_NAME=${AWS_ACCOUNT}.dkr.ecr.us-east-1.amazonaws.com/${PARENT_AGS,,}/${PARENT_COMPONENT}
export AWS_DEFAULT_REGION=us-east-1

cd ${WORKSPACE}/release

sudo $(aws ecr get-login --no-include-email --region us-east-1 --registry-ids ${AWS_ACCOUNT})

# Creating ECR repository if doesn't exists
aws ecr describe-repositories --repository-names ${PARENT_AGS,,}/${PARENT_COMPONENT}
if [[ $? -ne 0 ]]; then
	echo "ECR repository doesn't exists, creating new one"
	aws ecr create-repository --repository-name ${PARENT_AGS,,}/${PARENT_COMPONENT}
else
	echo "ECR repository exists, skipping creation"
fi

sudo docker load -i $PARENT_COMPONENT.tar
sudo docker images

#add latest tag to new image 
sudo docker tag $PARENT_COMPONENT:latest ${IMAGE_NAME}:latest
sudo docker push ${IMAGE_NAME}:latest

#add semantic version tag to new image
sudo docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_VERSION}
sudo docker push ${IMAGE_NAME}:${IMAGE_VERSION}

#add build & time tag to the new image
sudo docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${IMAGE_VERSION}-${PARENT_IMAGE_SUFFIX}
sudo docker push ${IMAGE_NAME}:${IMAGE_VERSION}-${PARENT_IMAGE_SUFFIX}

sudo docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${PARENT_IMAGE_SUFFIX}
sudo docker push ${IMAGE_NAME}:${PARENT_IMAGE_SUFFIX}

aws ecr describe-images --repository-name ${PARENT_AGS,,}/${PARENT_COMPONENT} --query 'sort_by(imageDetails,& imagePushedAt)[-1].imageTags[0]' --output text
