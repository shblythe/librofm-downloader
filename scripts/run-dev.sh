#!/bin/sh
[ ! -f .env ] || export $(grep -v '^#' .env| xargs)
server/build/install/server/bin/server run ${VERBOSE:+-v} ${DRY_RUN:+-n} ${RENAME_CHAPTERS:+--rename-chapters} --libro-fm-username "${LIBRO_FM_USERNAME}" --libro-fm-password "${LIBRO_FM_PASSWORD}" --data-dir "./run/data/" --media-dir "./run/media"
