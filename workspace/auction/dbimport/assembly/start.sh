#!/bin/sh
java -server -Xms2048m -Xmx2048m -classpath "app:config:lib/*" -Dlogback.configurationFile=logback-prod.xml mrwolf.dbimport.Starter