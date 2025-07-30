#!/bin/bash

# Minecraft 1.2.5 Download Script
# This script downloads the official Minecraft 1.2.5 client JAR file

echo "Minecraft 1.2.5 Download Script"
echo "================================"
echo ""

# Create lib directory if it doesn't exist
mkdir -p lib

# Minecraft 1.2.5 JAR URL (official Mojang server)
MINECRAFT_URL="https://piston-data.mojang.com/v1/objects/4a2fac7504182a97dcbcd7560c6392d7c8139928/client.jar"
JAR_FILE="lib/minecraft-1.2.5.jar"

echo "Downloading Minecraft 1.2.5 client JAR..."
echo "URL: $MINECRAFT_URL"
echo "Destination: $JAR_FILE"
echo ""

# Check if curl is available
if ! command -v curl &> /dev/null; then
    echo "Error: curl is not installed. Please install curl and try again."
    exit 1
fi

# Download the JAR file
echo "Starting download..."
if curl -L -o "$JAR_FILE" "$MINECRAFT_URL"; then
    echo ""
    echo "✅ Download completed successfully!"
    echo "File saved to: $JAR_FILE"
    
    # Show file size
    if command -v ls &> /dev/null; then
        echo "File size: $(ls -lh "$JAR_FILE" | awk '{print $5}')"
    fi
    
    echo ""
    echo "You can now run the Minecraft browser application:"
    echo "  npm run dev"
    echo "  Then open http://localhost:8080 in your browser"
    
else
    echo ""
    echo "❌ Download failed!"
    echo "Please check your internet connection and try again."
    echo ""
    echo "Alternative: You can manually download the Minecraft 1.2.5 client JAR"
    echo "from a legal source and place it in the lib/ folder as 'minecraft-1.2.5.jar'"
    exit 1
fi

echo ""
echo "Note: This script downloads the official Minecraft client from Mojang's servers."
echo "You must have a legal copy of Minecraft to use this application." 