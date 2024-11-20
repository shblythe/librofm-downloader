#!/bin/sh
bin/server run ${DRY_RUN:+-n} --libro-fm-username "${LIBRO_FM_USERNAME}" --libro-fm-password "${LIBRO_FM_PASSWORD}"