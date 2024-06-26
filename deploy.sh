#!/bin/bash
set -x


# Usage: deploy.sh <environment name> Example: deploy.sh qa --dry-run
# This script can only be executed when we are logged into the admin environment

service_name="sandbox"
secrets_path="/tmp/$service_name-$ENVIRONMENT"

ENVIRONMENT=$1
NAMESPACE=$2

if [ $ENVIRONMENT == "prod" -o $ENVIRONMENT == "staging" ]; then
     region="ap-south-1"
else
     region="us-east-1"
fi

    if [ ! -d "$HOME/sandbox-service" ]; then
        echo "No such directory: $HOME/sandbox-service/"
        exit 1
    fi

if [ $ENVIRONMENT == "prod" -o $ENVIRONMENT == "staging" ]; then
	registry="274334742953.dkr.ecr.ap-south-1.amazonaws.com/bb-engg/"
elif [ $PROJECT == "bb-stable" ]; then
    registry="274334742953.dkr.ecr.ap-south-1.amazonaws.com/bb-engg/"
elif [ $ENVIRONMENT == "local" ]; then
    registry=""
else
	registry="274334742953.dkr.ecr.us-east-1.amazonaws.com/bb-engg/"
fi

mkdir -p /tmp/$service_name-$ENVIRONMENT-$PROJECT
secrets_path="/tmp/$service_name-$ENVIRONMENT-$PROJECT"

if [[ "${ENVIRONMENT}" == "prod" || "${ENVIRONMENT}" == "staging" ]]; then
    echo -e "pulling $ENVIRONMENT app secrets"
    aws s3 cp s3://bbconf-india/micro-svcs/$service_name/$ENVIRONMENT/conf/values-secrets.yaml $secrets_path --region $region
    echo -e "pulling $ENVIRONMENT infra secrets"
    aws s3 cp s3://bbconf-india/$ENVIRONMENT/infra-settings/values-infra-secrets.yaml $secrets_path/values-infra-secrets.yaml --region $region
elif [ "${ENVIRONMENT}" == "dev" -o "${ENVIRONMENT}" == "qa" -o "${ENVIRONMENT}" == "hqa" -o "${ENVIRONMENT}" == "uat" ]; then
    echo -e "pulling $ENVIRONMENT app secrets"
    aws s3 cp s3://qa-conf/msvc/$service_name/$ENVIRONMENT/conf/values-secrets.yaml $secrets_path --region $region
    echo -e "pulling $ENVIRONMENT infra secrets"
    aws s3 cp s3://qa-conf/$ENVIRONMENT/infra-settings/values-infra-secrets-$ENVIRONMENT.yaml $secrets_path/values-infra-secrets.yaml --region $region
else
    echo -e "Can not pull any secrets."
fi

echo -e "Running Heva..."
cd $HOME/sandbox-service/charts/sandbox
printf "Heva version: $(heva -v)"
echo "Merging all values files into one $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml"
output_file_name="$service_name-final-$ENVIRONMENT-values.yaml"
if [[ -z "${PROJECT}" ]]; then
  echo "No 'Project' Environment variable found! Going with default/stable."
  heva \
    -f values.yaml \
    -f $secrets_path/values-secrets.yaml \
    -f $secrets_path/values-infra-secrets.yaml \
    -f env-overrides/values-$ENVIRONMENT.yaml \
    -o $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml 2>&1
else
  echo "Project=$PROJECT Environment variable found. Going with that."
  heva \
    -f values.yaml \
    -f $secrets_path/values-secrets.yaml \
    -f $secrets_path/values-infra-secrets.yaml \
    -f env-overrides/values-$ENVIRONMENT.yaml \
    -f project-overrides/values-$ENVIRONMENT-$PROJECT.yaml \
    -o $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml 2>&1
fi
echo "Final Helm values file content:"
cat "$secrets_path/$output_file_name"

echo -e "Rendering helm templates based on supplied values file:"
helm template -v 5 \
--namespace $NAMESPACE \
--set namespace=${NAMESPACE} \
--logtostderr \
--set registry=${registry} \
--debug \
--values $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml \
. 2>&1

if [[ $? == 0 ]];then
      echo -e "the value of dry is $DRY_RUN"
      if [[  ${DRY_RUN} == true ]]; then
        echo -e "Runinng dry run"
        if [[ -z "${PROJECT}" ]]; then
            helm upgrade \
                -v 5 \
                --dry-run \
                --namespace $NAMESPACE \
                --set namespace=${NAMESPACE} \
                --logtostderr \
                --set registry=${registry} \
                --debug \
                --install \
                --atomic  \
                --timeout 1500s \
                --cleanup-on-fail \
                --values $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml \
                $service_name \
                . 2>&1
        else
            helm upgrade \
                -v 5 \
                --dry-run \
                --namespace $NAMESPACE \
                --set namespace=${NAMESPACE} \
                --logtostderr \
                --set registry=${registry} \
                --debug \
                --install \
                --atomic  \
                --timeout 1500s \
                --cleanup-on-fail \
                --values $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml \
                $service_name-$PROJECT \
                . 2>&1
        fi
    else
        echo "Doing Release!"
        if [[ -z "${PROJECT}" ]]; then
            helm upgrade \
                -v 5 \
                --namespace $NAMESPACE \
                --set namespace=${NAMESPACE} \
                --logtostderr \
                --set registry=${registry} \
                --debug \
                --install \
                --atomic  \
                --timeout 1500s \
                --cleanup-on-fail \
                --values $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml \
                $service_name \
                . 2>&1
        else
            helm upgrade \
                -v 5 \
                --namespace $NAMESPACE \
                --set namespace=${NAMESPACE} \
                --logtostderr \
                --set registry=${registry} \
                --debug \
                --install \
                --atomic  \
                --timeout 1500s \
                --cleanup-on-fail \
                --values $secrets_path/$service_name-final-$ENVIRONMENT-values.yaml \
                $service_name-$PROJECT \
                . 2>&1
        fi
    fi
else
      echo "Helm Template validation failed. Hence release is halted!"
fi