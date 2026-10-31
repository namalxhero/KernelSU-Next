#!/system/bin/sh
# Nova Boot Guard: count boots; after 3 boots that never finished, disable every other module.
MODDIR=${0%/*}
CNT_FILE=/data/adb/nova_boot_count
CNT=$(cat "$CNT_FILE" 2>/dev/null)
[ -z "$CNT" ] && CNT=0
CNT=$((CNT + 1))
echo "$CNT" > "$CNT_FILE"
if [ "$CNT" -ge 3 ]; then
  for m in /data/adb/modules/*; do
    [ -d "$m" ] || continue
    [ "$m" = "$MODDIR" ] && continue
    touch "$m/disable"
  done
  echo 0 > "$CNT_FILE"
  date > /data/adb/nova_rescue_triggered
fi
