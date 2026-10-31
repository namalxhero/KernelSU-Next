#!/system/bin/sh
# Boot finished and stayed up for a while: reset the failed boot counter.
while [ "$(getprop sys.boot_completed)" != "1" ]; do
  sleep 5
done
sleep 60
echo 0 > /data/adb/nova_boot_count
