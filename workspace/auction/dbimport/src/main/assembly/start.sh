#!/bin/sh
java -classpath "app:config:lib/*" -Dlogback.configurationFile=logback-prod.xml ch.mrwolf.wow.dbimport.Starter