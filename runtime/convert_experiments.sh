#!/bin/bash

shopt -s globstar

jar_dir="$(dirname $0)/target/propcov-runtime-0.1-SNAPSHOT-jar-with-dependencies.jar"
config_dir="$(dirname $0)/../artifacts/configs"

for src in "$config_dir"/PropCovExperiments/*.yaml
do
  if [ ! -f "$src.old" ]
  then
    mv "$src" "$src.old"
  fi
done


for src in "$config_dir"/PropCovExperiments/*.yaml.old
do
  tgt=${src::-4}
  java -cp "$jar_dir" edu.uic.bitslab.propcov.core.config.ConverterExperiment "$src" "$tgt" $1
done

