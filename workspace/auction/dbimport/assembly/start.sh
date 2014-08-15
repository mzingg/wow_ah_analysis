#!/bin/sh
java -classpath "app:config:lib/*" -Dlogback.configurationFile=logback-prod.xml mrwolf.dbimport.Starter