package templates.ecs

ags             = "CRISP"  // uppercase
component       = "lambda-java"    // lowercase
appName         = "${ags}-${component}"
timeout         = 1800000
region          = "east"
launchType      = "lambda"

tags {
    ags = "CRISP"
    costCenter = "CRP900"
    component = "lambda"
}

lambdas {
    global_configuration {
        memory = "2048"
        timeout = "900"
        forceUpdate = true
        executionRole = "SVC_LAMBDA_CRISP_SR"
        logRetentionPeriod = "14"
        description = "Lambda to execute crisp java based jobs scripts"
        subscriptionFilter {
            deliveryStreamName = "SPLUNK-LAMBDA-CRISP"
            role = "SVC_CW_FINRA_LAMBDA_SPLUNK_SR"
        }
    }
}

environments {
    "dev" {
        tags {
            sdlc = "DEV"
        }
        lambdas {
             crispJava {
                name = "CRISP-DEV-JAVA-LAMBDA"
                createLogGroup = true
                permissions {
                  //grant tools-dev account invoke privileges
                    permissionJava1 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::754249303703:role/APP_JAMS"
                    }
                }
				image {
                    imageURI = "465257512377.dkr.ecr.us-east-1.amazonaws.com/crisp/crisp-lambda-java:" + (System.getenv("PARENT_IMAGE_SUFFIX") ?: 'latest') 
                    command = "org.finra.crisp.lambda.handlers.BasicHandler"
                }
                environment {
                    variables = ["SDLC": "dev"]
                }
                vpcConfig {
                    subnetId = ["subnet-0df4547a"]
                    securityGroupId = ["${lookup.sg("finra-outbound", "Dev_East")}", "${lookup.sg("finra-support", "Dev_East")}","${lookup.sg("CRISP-internal", "Dev_East")}","${lookup.sg("CRISP-client-protected", "Dev_East")}"]
                }
            }
        }
    }
    "dev-int" {
        tags {
            sdlc = "DEV-INT"
        }
        lambdas {
             crispJava {
                name = "CRISP-DEV-INT-JAVA-LAMBDA"
                createLogGroup = true
                permissions {
                  //grant tools-dev account invoke privileges
                    permissionJava2 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::754249303703:role/APP_JAMS"
                    }
                }
				image {
                    imageURI = "465257512377.dkr.ecr.us-east-1.amazonaws.com/crisp/crisp-lambda-java:" + (System.getenv("PARENT_IMAGE_SUFFIX") ?: 'latest')
                    command = "org.finra.crisp.lambda.handlers.BasicHandler"
                }
                environment {
                    variables = ["SDLC": "dev-int"]
                }
                vpcConfig {
                    subnetId = ["subnet-1d2e3435", "subnet-0df4547a"]
                    securityGroupId = ["${lookup.sg("finra-outbound", "Dev_East")}", "${lookup.sg("finra-support", "Dev_East")}","${lookup.sg("CRISP-internal", "Dev_East")}","${lookup.sg("CRISP-client-protected", "Dev_East")}"]
                }
            }
        }
    }
    "qa" {
        tags {
            sdlc = "QA"
        }
        lambdas {
            crispJava {
                name = "CRISP-QA-JAVA-LAMBDA"
              	// un-comment creation of log group the first time a particular lambda is provisioned
              	createLogGroup = true
                permissions {
                  // grant tools-qa account invoke privileges
                  permissionJava1 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::043811358421:role/APP_JAMS"
                    }
                  permissionJavaDaslConsole1 {
                     	 action = "lambda:InvokeFunction"
                         principal = "arn:aws:iam::142248000760:role/priv_aws_dasl_dev_q"
                    }
                   permissionJavaCrispConsole1 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::142248000760:role/priv_aws_crisp_dev_q"
                    }
                }
				image {
                    imageURI = "142248000760.dkr.ecr.us-east-1.amazonaws.com/crisp/crisp-lambda-java:" + (System.getenv("PARENT_IMAGE_SUFFIX") ?: 'latest')
                    command = "org.finra.crisp.lambda.handlers.BasicHandler"
                }
                environment {
                    variables = ["SDLC": "qa"]
                }
                vpcConfig {
                    subnetId = ["subnet-d4c765a3", "subnet-3ee52867", "subnet-184da033"]
                    securityGroupId = ["${lookup.sg("finra-outbound", "Qa_East")}", "${lookup.sg("finra-support", "Qa_East")}","${lookup.sg("CRISP-internal", "Qa_East")}","${lookup.sg("CRISP-client-protected", "Qa_East")}"]
                }

            }
        }
        }
      "qa-int" {
        tags {
            sdlc = "QA-INT"
        }
        lambdas {
            crispJava {
                name = "CRISP-QA-INT-JAVA-LAMBDA"
              	// un-comment creation of log group the first time a particular lambda is provisioned
              	createLogGroup = true
                permissions {
                  // grant tools-qa account invoke privileges
                  permissionJava2 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::043811358421:role/APP_JAMS"
                    }
                  permissionJavaDaslConsole2 {
                     	 action = "lambda:InvokeFunction"
                         principal = "arn:aws:iam::142248000760:role/priv_aws_dasl_dev_q"
                    }
                   permissionJavaCrispConsole2 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::142248000760:role/priv_aws_crisp_dev_q"
                    }
                }
				image {
                    imageURI = "142248000760.dkr.ecr.us-east-1.amazonaws.com/crisp/crisp-lambda-java:"  + (System.getenv("PARENT_IMAGE_SUFFIX") ?: 'latest')
                    command = "org.finra.crisp.lambda.handlers.BasicHandler"
                }
                environment {
                    variables = ["SDLC": "qa-int"]
                }
                vpcConfig {
                    subnetId = ["subnet-d4c765a3", "subnet-3ee52867", "subnet-184da033"]
                    securityGroupId = ["${lookup.sg("finra-outbound", "Qa_East")}", "${lookup.sg("finra-support", "Qa_East")}","${lookup.sg("CRISP-internal", "Qa_East")}","${lookup.sg("CRISP-client-protected", "Qa_East")}"]
                }

            }
        }
    }
    "prod" {
        tags {
            sdlc = "PROD"
        }
        lambdas {
            crispJava {
                name = "CRISP-PROD-JAVA-LAMBDA"
               // un-comment creation of log group the first time a particular lambda is provisioned
              	createLogGroup = true
                permissions {
                  // grant tools-qa account invoke privileges
                    permissionJava1 {
                        action = "lambda:InvokeFunction"
                        principal = "arn:aws:iam::570164370074:role/APP_JAMS"
                    }
                }
				image {
                    imageURI = "510199193688.dkr.ecr.us-east-1.amazonaws.com/crisp/crisp-lambda-java:"  + (System.getenv("PARENT_IMAGE_SUFFIX") ?: 'latest')
                    command = "org.finra.crisp.lambda.handlers.BasicHandler"
                }
                environment {
                    variables = ["SDLC": "prod"]

                }
                vpcConfig {
                    subnetId = ["subnet-acee27f5", "subnet-755bb25e", "subnet-85cf61f2"]
                    securityGroupId = ["${lookup.sg("finra-outbound", "Prod_East")}", "${lookup.sg("finra-support", "Prod_East")}","${lookup.sg("CRISP-internal", "Prod_East")}","${lookup.sg("CRISP-client-protected", "Prod_East")}"]
                }
            }
        }
    }
}
