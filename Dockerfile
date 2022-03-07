
# configure the build environment
##Pulls FINRA Java8 Full Package Base Image (Amazon Linux 2)
ARG BASE_IMAGE_TAG=2021.09-al2

FROM 465257512377.dkr.ecr.us-east-1.amazonaws.com/finra/java8:${BASE_IMAGE_TAG}
ARG BASE_IMAGE_TAG
RUN echo BASE_IMAGE_TAG:$BASE_IMAGE_TAG
USER root
EXPOSE 8080

ARG FUNCTION_LAUNCHER="/function_launcher"
RUN mkdir -p ${FUNCTION_LAUNCHER}
RUN echo ${FUNCTION_LAUNCHER}

# copy crisp generic lambda and aws lambda dependency jars to launcher folder
COPY ./crisp-lambda-handler/*.jar ${FUNCTION_LAUNCHER}/
COPY ./crisp-lambda-handler/log4j2.xml ${FUNCTION_LAUNCHER}/
RUN echo $(ls -lR /function_launcher)


# copy application jar to its folder
ARG FUNCTION_DM_CLIENT="/function_dm_client"
RUN mkdir -p ${FUNCTION_DM_CLIENT}
COPY ./crisp-dm-client/crisp-dm-client-0.0.1-SNAPSHOT-DM.jar ${FUNCTION_DM_CLIENT}/
RUN echo $(ls -lR /function_dm_client)

# copy plugins jars to .plugins folder. 
ARG PLUGINS="/.plugins"
RUN mkdir -p ${PLUGINS}
COPY plugins/ ${PLUGINS}/
RUN echo $(ls -lR /.plugins)

WORKDIR ${FUNCTION_LAUNCHER}

ENTRYPOINT [ "/usr/bin/java", "-cp", "./*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" ]
CMD [ "org.finra.crisp.lambda.handlers.BasicHandler" ]
