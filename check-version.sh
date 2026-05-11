#!/usr/bin/env sh

BASE_URL="https://packages.termux.dev/apt/termux-main"

if [ -z "$1" ]; then
  echo "Usage: sh check-version.sh <package-name> [arch]"
  echo "Example: sh check-version.sh nodejs-lts aarch64"
  exit 1
fi

PKG="$1"
ARCH="${2:-aarch64}"

curl -Ls "$BASE_URL/dists/stable/main/binary-${ARCH}/Packages" | \
  awk -v pkg="$PKG" '$1=="Package:" {p=$2} p==pkg && $1=="Version:" {print $2; exit}'