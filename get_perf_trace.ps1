$DEVICE_ID = "adb-38301FDJG008F2-nmkcrw._adb-tls-connect._tcp"

$config = @"
buffers: {
    size_kb: 63488
    fill_policy: RING_BUFFER
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "sched_switch"
            ftrace_events: "print"
            atrace_categories: "app"
            atrace_categories: "gfx"
            atrace_categories: "view"
            atrace_apps: "com.kouhee.imagebenchmark"
        }
    }
}
duration_ms: 10000
"@

Write-Host "Starting trace (10s) on $DEVICE_ID..."
$config | adb -s $DEVICE_ID shell perfetto -c - --txt -o /data/misc/perfetto-traces/trace.perfetto-trace

Write-Host "Pulling trace file..."
adb -s $DEVICE_ID pull /data/misc/perfetto-traces/trace.perfetto-trace ./trace.perfetto-trace

Write-Host "Done. Open ./trace.perfetto-trace at https://ui.perfetto.dev/"