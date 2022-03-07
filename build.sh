#!/bin/bash
set -e
set -x
#build.sh
# ##############################################################################
# Script would be executed during application build time
#
# FINRA , 2018
###############################################################################


###create release and staging dir's#####
mkdir -p ${WORKSPACE}/release
mkdir -p ${WORKSPACE}/archive


mkdir -p ${WORKSPACE}/release/crisp-lambda-handler
mkdir -p ${WORKSPACE}/release/plugins
mkdir -p ${WORKSPACE}/release/plugins/crisp-lambda-plugin-hotdeploy



cp ${WORKSPACE}/checkout/java/bdhub.properties ${WORKSPACE}/release/


cd ${WORKSPACE}/checkout/java
export JAVA_HOME=/apps/jdk/jdk1.8.0
export M2_HOME=/apps/maven/apache-maven-3.3.3
export M2=/apps/maven/apache-maven-3.3.3/bin
export PATH=/apps/maven/apache-maven-3.3.3/bin:$PATH
mvn clean install -f "./pom.xml" -s /tmp/settings.xml

cp $WORKSPACE/checkout/java/crisp-lambda-handler/target/dependency/*.jar ${WORKSPACE}/release/crisp-lambda-handler
cp $WORKSPACE/checkout/java/crisp-lambda-handler/target/dependency/*.jar ${WORKSPACE}/archive

cp $WORKSPACE/checkout/java/crisp-lambda-handler/target/*.jar ${WORKSPACE}/release/crisp-lambda-handler
cp $WORKSPACE/checkout/java/crisp-lambda-handler/target/*.jar ${WORKSPACE}/archive

# logging objects are initialized during class loading of our generic lambda launcher. The below log4j2.xml is the global logging
# configuration for all applications.
cp $WORKSPACE/checkout/java/crisp-lambda-handler/src/main/resources/log4j2.xml ${WORKSPACE}/release/crisp-lambda-handler

#copy hotdeploy plugin jars
cp $WORKSPACE/checkout/java/crisp-lambda-plugin-hotdeploy/target/dependency/*.jar ${WORKSPACE}/release/plugins/crisp-lambda-plugin-hotdeploy
cp $WORKSPACE/checkout/java/crisp-lambda-plugin-hotdeploy/target/dependency/*.jar ${WORKSPACE}/archive
cp $WORKSPACE/checkout/java/crisp-lambda-plugin-hotdeploy/target/*.jar ${WORKSPACE}/release/plugins/crisp-lambda-plugin-hotdeploy
cp $WORKSPACE/checkout/java/crisp-lambda-plugin-hotdeploy/target/*.jar ${WORKSPACE}/archive


cp -r ${WORKSPACE}/checkout/java/*.sh ${WORKSPACE}/release
cp -r ${WORKSPACE}/checkout/java/Dockerfile ${WORKSPACE}/release
cp -r ${WORKSPACE}/checkout/java/app.groovy ${WORKSPACE}/release

