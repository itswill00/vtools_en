#!/bin/bash

# ==========================================
#  SCENEX PRO BUILDER (Interactive & Notify)
# ==========================================

# --- Configuration ---
TOKEN="8780321748:AAFBdXQW8JWeR2lr3cpsyrydZz62lXBOExg"
CHAT_ID="-1003322434219"
PROJECT_NAME="SceneX"
VARIANT="debug"
APK_DIR="app/build/outputs/apk/$VARIANT"

# --- Colors for Terminal ---
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# --- Utilities ---
log_header() { echo -e "\n${CYAN}==== $1 ====${NC}"; }
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

send_telegram_msg() {
    curl -s -X POST "https://api.telegram.org/bot$TOKEN/sendMessage" \
        -d chat_id="$CHAT_ID" \
        -d parse_mode="HTML" \
        -d text="$1" > /dev/null
}

# --- Initialization ---
START_TIME=$(date +%s)
GIT_REV=$(git rev-parse --short HEAD)
VERSION_NAME=$(grep "versionName" app/build.gradle | head -n 1 | awk -F'"' '{print $2}')

log_header "PREPARING BUILD"
log_info "Project: $PROJECT_NAME (v$VERSION_NAME)"
log_info "Commit: $GIT_REV"
log_info "Variant: $VARIANT"

# Notify Telegram: Build Started
send_telegram_msg "Build Started
Project: $PROJECT_NAME
Version: <code>$VERSION_NAME</code>
Commit: <code>$GIT_REV</code>
Status: Compiling..."

# Handle Clean Argument
CLEAN_CMD=""
for arg in "$@"; do
    if [ "$arg" == "--clean" ]; then 
        log_info "Clean mode enabled."
        CLEAN_CMD="clean"
    fi
done

# --- Execution ---
log_header "GRADLE EXECUTION (LOGS BELOW)"

./gradlew $CLEAN_CMD ":app:assemble${VARIANT^}" -x lint
GRADLE_STATUS=$?

if [ $GRADLE_STATUS -ne 0 ]; then
    log_error "Build failed with exit code $GRADLE_STATUS"
    send_telegram_msg "Build Failed
Project: $PROJECT_NAME
Status: Compilation Error
Log: Please check terminal output."
    exit $GRADLE_STATUS
fi

# --- Success Process ---
log_header "POST-BUILD ANALYSIS"
APK_FILE=$(find "$APK_DIR" -name "*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    log_error "APK file not found in $APK_DIR"
    exit 1
fi

# Metadata
TIMESTAMP=$(date +"%m%d_%H%M")
NEW_NAME="${PROJECT_NAME}_v${VERSION_NAME}_DEBUG_${TIMESTAMP}.apk"
cp "$APK_FILE" "./$NEW_NAME"

FILE_SIZE=$(du -sh "$NEW_NAME" | awk '{print $1}')
SHA256_VAL=$(sha256sum "$NEW_NAME" | awk '{print $1}')

DURATION=$(( $(date +%s) - START_TIME ))
CHANGELOG=$(git log -n 5 --pretty=format:"* %s" | sed 's/&/\&amp;/g; s/</\&lt;/g; s/>/\&gt;/g')

# --- Uploading ---
log_info "Uploading $NEW_NAME ($FILE_SIZE) to Telegram..."

MESSAGE="Build Successful

Project: $PROJECT_NAME
Version: <code>$VERSION_NAME</code>
Commit: <code>$GIT_REV</code>

Recent Changes:
$CHANGELOG

Stats:
- Size: <code>$FILE_SIZE</code>
- Duration: <code>$((DURATION/60))m $((DURATION%60))s</code>
- Type: <code>$VARIANT</code>

SHA256 Checksum:
<code>$SHA256_VAL</code>"

RESPONSE=$(curl -s -F chat_id="$CHAT_ID" \
    -F document=@"$NEW_NAME" \
    -F caption="$MESSAGE" \
    -F parse_mode="HTML" \
    "https://api.telegram.org/bot$TOKEN/sendDocument")

if echo "$RESPONSE" | grep -q '"ok":true'; then
    log_success "Build $VERSION_NAME uploaded successfully!"
    rm "./$NEW_NAME"
else
    log_error "Upload failed!"
    echo "$RESPONSE"
    rm -f "./$NEW_NAME"
    exit 1
fi
