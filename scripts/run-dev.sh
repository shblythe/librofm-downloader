#!/bin/sh
[ ! -f .env ] || export $(grep -v '^#' .env| xargs)
server/build/install/server/bin/server run ${DRY_RUN:+-n} --libro-fm-username "${LIBRO_FM_USERNAME}" --libro-fm-password "${LIBRO_FM_PASSWORD}" --data-dir "./run/data/" --media-dir "./run/media"
