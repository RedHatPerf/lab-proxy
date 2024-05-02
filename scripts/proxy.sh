#!/bin/bash

touch ./ENV

podman pod create proxy-service
podman run -dt --pod proxy-service --env-file ./ENV --name lab-proxy -v $JOBS_DIR:/jobs:z  quay.io/app-svc-perf/lab-proxy:dev
podman run -dt --pod proxy-service --name relay-proxy docker.io/webhookrelay/webhookrelayd:latest  -k $RELAY_KEY -s $RELAY_SECRET -b $RELAY_BACKEND
