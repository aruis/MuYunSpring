#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
central_repository="${MUYUN_CENTRAL_REPOSITORY:-https://repo.maven.apache.org/maven2}"
group_path="net/ximatai/muyun/spring"
version="${MUYUN_RELEASE_VERSION:-$(awk -F= '/^muyunVersion=/ { print $2; exit }' "$repository_root/gradle.properties")}"
bom_url="$central_repository/$group_path/muyun-spring-bom/$version/muyun-spring-bom-$version.pom"

for _ in {1..90}; do
  if curl --fail --silent --output /dev/null "$bom_url"; then
    MUYUN_CONSUMER_REPOSITORY="$central_repository" MUYUN_CONSUMER_VERSION="$version" \
      "$repository_root/scripts/verify-published-consumer.sh"
    exit 0
  fi
  sleep 4
done

echo "Maven Central did not expose $bom_url within 6 minutes." >&2
exit 1
