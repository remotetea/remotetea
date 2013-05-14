#!/bin/bash

set -e

SF_PROJECT=remotetea
GITHUB_ORIGIN=git@github.com:remotetea/remotetea.git

rm -rf cvs2git-*.dat new-repo cvs-bak

rsync -av rsync://${SF_PROJECT}.cvs.sourceforge.net/cvsroot/${SF_PROJECT}/* cvs-bak

cvs2git --options=cvs2git.options

mkdir new-repo
cd new-repo
git init --bare

cat ../cvs2git-blob.dat ../cvs2git-dump.dat | git fast-import

git remote add origin ${GITHUB_ORIGIN}
git push -uf origin master
git push -f --all
git push -f --tags

