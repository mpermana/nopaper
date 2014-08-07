#!/bin/bash
git pull
echo Shutting down
curl http://localhost:4567/npserver/system/shutdown


if [ "taru" = "`hostname`" ]; then
	export NPDATA=/home/mpermana/projects/nopaper/data
fi

nohup mvn$Debug compile exec:java -Dexec.mainClass=nopaper.Server -Ddatabase.hostname=localhost &

while true; do
	curl http://localhost:4567/npserver/
	if [ 0 = $? ]; then
		break;
	fi
	sleep 1;
done

