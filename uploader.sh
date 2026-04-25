#!/bin/bash

# Telegram Configuration
TOKEN="8780321748:AAFBdXQW8JWeR2lr3cpsyrydZz62lXBOExg"
CHAT_ID="-1003322434219"

# Path Configuration
APK_PATH="app/build/outputs/apk/release_mini"
APK_FILE=$(find "$APK_PATH" -name "*.apk" | head -n 1)

if [ -z "$APK_FILE" ]; then
    echo "Error: APK file not found!"
    exit 1
fi

# Version and Info
GIT_REV=$(git rev-parse --short HEAD)
TIMESTAMP=$(date +"%m%d_%H%M")
VERSION_NAME="5.0-r1812"

# Create Unique Filename
NEW_NAME="SceneX_v${VERSION_NAME}_${GIT_REV}_${TIMESTAMP}.apk"
cp "$APK_FILE" "./$NEW_NAME"

# Checksum
SHA256=$(sha256sum "$NEW_NAME" | awk '{print $1}')

# Changelog (Clean text)
CHANGELOG=$(git log -n 5 --pretty=format:"- %s" | sed 's/<[^>]*>//g')

# Format Message
MESSAGE="📦 *New Build Uploaded!*

*File:* $NEW_NAME
*Version:* $VERSION_NAME ($GIT_REV)
*Date:* $(date +'%Y-%m-%d %H:%M:%S')

*🛠 Changelogs:*
$CHANGELOG

*🔐 Checksum (SHA256):*
`$SHA256`"

echo "Uploading $NEW_NAME to Telegram..."

# Use Markdown (v1 style for better compatibility with curl)
RESPONSE=$(curl -s -F chat_id="$CHAT_ID" \
    -F document=@"$NEW_NAME" \
    -F caption="$MESSAGE" \
    -F parse_mode="Markdown" \
    "https://api.telegram.org/bot$TOKEN/sendDocument")

if echo "$RESPONSE" | grep -q '"ok":true'; then
    echo "Success: File uploaded successfully!"
    rm "./$NEW_NAME"
else
    echo "Error: Upload failed!"
    echo "$RESPONSE"
    rm "./$NEW_NAME"
    exit 1
fi
