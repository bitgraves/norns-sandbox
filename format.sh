#!/bin/bash
#
# norns maiden makes it difficult to tell whether spaces and tabs are consistent.
# run this to convert all .sc and .lua files to 2-space indentation.

set -e

if git rev-parse --git-dir > /dev/null 2>&1; then
    echo "inside a git repo, continuing"
else
    echo "not inside a git repo, exiting"
    exit 1
fi

echo "formatting *.sc ..."
find . -name "*.sc" -print0 | while read -d $'\0' i
do
    echo "  $i"
    expand -t 2 $i >> $i.spaces
    mv $i.spaces $i
done

echo "formatting *.lua ..."
find . -name "*.lua" -print0 | while read -d $'\0' i
do
    echo "  $i"
    expand -t 2 $i >> $i.spaces
    mv $i.spaces $i
done
