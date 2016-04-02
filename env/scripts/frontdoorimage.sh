#!/bin/sh

if [ -z "$1" ]
  then
    FILENAME="/tmp/snapshot.jpg"
  else
    FILENAME=$1;
fi

if [ -z "$2" ]
  then
    STREAMID="0";
  else
    STREAMID=$2;
fi

/usr/local/bin/ffmpeg -y -i rtsp://192.168.1.10:554/user=admin_password=tlJwpbo6_channel=1_stream=$STREAMID.sdp?real_stream -ss 00:00:01.000 -f image2 -vframes 1 $FILENAME 2>/tmp/ffmpegLog.txt
echo "done"
