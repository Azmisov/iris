# clears debug logs from dev folder
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
pushd "$parent_path"
find ../dev/logs/log -type f -exec truncate -s0 {} +
popd