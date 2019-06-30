#!/usr/bin/env bash

set -eux

echo $GPG_PRIVATE_KEY_B64 | base64 --decode > gpg_key

gpg --import gpg_key

rm gpg_key

./mill -i mill.scalalib.PublishModule/publishAll \
    lihaoyi:$SONATYPE_PASSWORD \
    $GPG_PASSWORD \
    --signed true \
    --publishArtifacts __.publishArtifacts \
    --release true


./mill uploadToGithub $GITHUB_ACCESS_TOKEN
