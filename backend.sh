#!/bin/bash
set -x
#todo if same shell is running ,exist


#make  sure output in shell's directory , than other shells can work correctly
BASEDIR=$(dirname "$0")
#echo "$BASEDIR"
cd "$BASEDIR"

directory= ""

if [[ -z "${JenkinsJobDir}" ]]; then
	 directory="../jobs"
else
  directory="${JenkinsJobDir}"
fi
echo $directory
export LANG=C
#make sure allbuilds exist
touch allbuilds

# setting LANG=C to make sure sort result are same in all files 
ls -dl --time-style="+%s" -R  ${directory}/*/builds/*|grep -v last|grep -v legacy|awk '{print $7"\t"$6}' >temp
#ls -dl --time-style=long-iso -R  ${directory}/*/builds/*|grep -v last|grep -v legacy|awk '{print $8"\t"$6" "$7}' >temp
sort temp -o temp

#append new builds to file allbuilds
comm -23 temp allbuilds >>allbuilds 
sort allbuilds -o allbuilds

#make sure allresults exist
touch allresults

#prepare compare file
cat allresults|awk '{print $1}' >temp2
sort temp2 -o temp2
cat allbuilds|awk '{print $1}' >temp3 ;
sort temp3 -o temp3

comm -23 temp3 temp2|awk '{print $1"/build.xml"}'|xargs grep -H "^  <result>" \
 |awk -F"/" '{print $1"/"$2"/"$3"/"$4"/"$5">"$6 }'|awk -F ">" '{print $1"\t"substr($3,0,length($3)-1)}' \
	>>allresults 
sort allresults -o allresults



  
