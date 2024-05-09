#!/bin/bash

touch ./ENV

podman pod create --publish 8085:8080 --publish 16686:16686 --publish 4317:4317 proxy-service
podman run -dt --pod proxy-service --env-file ./ENV --name lab-proxy -v $JOBS_DIR:/jobs:z  quay.io/app-svc-perf/lab-proxy:dev
podman run -dt --pod proxy-service --name relay-proxy docker.io/webhookrelay/webhookrelayd:latest  -k $RELAY_KEY -s $RELAY_SECRET -b $RELAY_BACKEND
