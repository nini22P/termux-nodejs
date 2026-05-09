#!/usr/bin/env sh
set -e

BASE_URL="https://packages.termux.dev/apt/termux-main"
DOWNLOAD_DIR="./download"

CLEAN=0

while [ $# -gt 0 ]; do
  case "$1" in
    --path)
      DOWNLOAD_DIR="$2"
      shift 2
      ;;
    --clean)
      CLEAN=1
      shift
      ;;
    *)
      break
      ;;
  esac
done

if [ $# -lt 2 ]; then
  echo "Usage: sh termux-package-downloader.sh [--path <path>] [--clean] <package> <aarch64|arm|x86_64|i686>"
  echo "Options:"
  echo "  --path <path>  Base download path (default: ./download)"
  echo "  --clean        Clean unpack directory before downloading"
  echo "Example: sh termux-package-downloader.sh --path /tmp/deps --clean nodejs-lts aarch64"
  exit 1
fi

PKG="$1"
ARCH="$2"

UNPACK_DIR="${DOWNLOAD_DIR}/${ARCH}"
PKG_DIR="${DOWNLOAD_DIR}/pkg/${ARCH}"

if [ "$CLEAN" -eq 1 ]; then
  echo "==> Cleaning unpack directory: $UNPACK_DIR"
  rm -rf "$UNPACK_DIR"
fi

mkdir -p "$UNPACK_DIR" "$PKG_DIR"

PACKAGES_FILE="${DOWNLOAD_DIR}/pkg/packages-${ARCH}"

echo "==> Download Packages index for ${ARCH}"
curl -L "$BASE_URL/dists/stable/main/binary-${ARCH}/Packages" -o "$PACKAGES_FILE"

get_pkg_info() {
  awk -v pkg="$1" '
  $1=="Package:" {p=$2}
  p==pkg {
    if ($1=="Filename:") f=$2
    if ($1=="Depends:") d=substr($0, index($0,$2))
    if ($1=="SHA256:") s=$2
  }
  END {
    if (f) {
      print f
      print d
      print s
    }
  }' "$PACKAGES_FILE"
}

check_sha256() {
  file="$1"
  expected="$2"
  [ -f "$file" ] || return 1
  actual=$(sha256sum "$file" | awk '{print $1}')
  [ "$actual" = "$expected" ]
}

extract_deb() {
  deb="$1"
  echo "📦 Extract: $(basename "$deb")"
  ar p "$deb" data.tar.xz 2>/dev/null | tar -xJ -C "$UNPACK_DIR" && return
  echo "❌ Unsupported data.tar format in $deb"
  exit 1
}

DONE_PKGS=""
DOWNLOADED_DEBS=""

download_pkg() {
  pkg="$1"

  echo "$DONE_PKGS" | grep -qw "$pkg" && return
  DONE_PKGS="$DONE_PKGS $pkg"

  echo ""
  echo "=== 📦 ${pkg} ==="

  info=$(get_pkg_info "$pkg")
  filename=$(echo "$info" | sed -n '1p')
  depends=$(echo "$info" | sed -n '2p')
  sha256=$(echo "$info" | sed -n '3p')

  if [ -z "$filename" ]; then
    echo "⚠️  Skip missing package: $pkg"
    return
  fi

  url="${BASE_URL}/${filename}"

  deb_name=$(basename "$filename")
  deb_name=$(echo "$deb_name" | sed 's/[:]/_/g')

  deb_path="${PKG_DIR}/${deb_name}"

  echo "URL   : $url"
  echo "SHA256: $sha256"
  echo "Deps  : $depends"

  if check_sha256 "$deb_path" "$sha256"; then
    echo "⚡ Use cached .deb"
  else
    echo "⬇️  Downloading..."
    curl -L "$url" -o "$deb_path"
    if ! check_sha256 "$deb_path" "$sha256"; then
      echo "❌ SHA256 mismatch for $deb_name"
      rm -f "$deb_path"
      exit 1
    fi
  fi

  DOWNLOADED_DEBS="$DOWNLOADED_DEBS $deb_path"

  for dep in $(echo "$depends" | tr ',' '\n'); do
    dep=$(echo "$dep" | sed 's/(.*)//' | xargs)
    [ -z "$dep" ] && continue
    download_pkg "$dep"
  done
}

download_pkg "$PKG"

echo ""
echo "==> All packages downloaded. Starting extraction..."
for deb in $DOWNLOADED_DEBS; do
  extract_deb "$deb"
done

echo ""
echo "🎉 ALL DONE"
echo "📂 Download directory: $DOWNLOAD_DIR"
echo "📂 Unpack directory  : $UNPACK_DIR"
echo "🗂  PKG directory    : $PKG_DIR"
echo "📦 Total extracted   : $(echo "$DOWNLOADED_DEBS" | wc -w) packages"
