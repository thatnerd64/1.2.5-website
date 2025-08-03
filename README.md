# Minecraft 1.2.5 Browser

## What is it

Thanks to [CheerpJ](https://cheerpj.com), this project runs Minecraft 1.2.5 in the browser, without the requirement of having the JVM locally installed on the computer.

This is a simplified implementation inspired by [Browsercraft](https://github.com/leaningtech/browsercraft) that runs the unmodified Minecraft 1.2.5 client JAR from a local file.

## Features

- **Unmodified Minecraft 1.2.5**: Runs the original Minecraft client JAR untouched
- **Local JAR Storage**: Uses a local JAR file for faster loading
- **Mod Support**: Place Forge-compatible mods in the `/mods` folder
- **Simple Setup**: Just download the JAR and run
- **Browser-based**: No Java installation required
- **Offline Mode**: Uses an embedded `.minecraft` directory to avoid external downloads
- **Custom Multiplayer**: Connect to servers with any username without online authentication

## Notes

This is not an official Minecraft product. It is not approved by or associated with Mojang or Microsoft. You must have a legal copy of Minecraft 1.2.5 to use this application.

## Quick Start

### Prerequisites
- Modern web browser with WebGL support (Chrome, Firefox, Safari, Edge)
- Node.js (for development server)
- Legal copy of Minecraft 1.2.5

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/minecraft-1.2.5-browser.git
   cd minecraft-1.2.5-browser
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Download Minecraft JAR**
   ```bash
   ./download-minecraft.sh
   ```
   
   Or manually download the Minecraft 1.2.5 client JAR and place it as `lib/lwjgl/minecraft-1.2.5.jar`

4. **Start the development server**
   ```bash
   npm run dev
   ```

5. **Open in browser**
   Navigate to `http://localhost:8080` in your web browser.

6. **Play Minecraft**
   - Accept the Minecraft EULA
   - (Optional) Enter a server address and port to join multiplayer
   - Click "Play Minecraft!" to start the game

## JAR File Setup

The application requires the Minecraft 1.2.5 client JAR file to be placed in the `lib/` folder.

### Automatic Download
Run the provided script to download the official JAR:
```bash
./download-minecraft.sh
```

### Manual Setup
1. Download the Minecraft 1.2.5 client JAR from a legal source
2. Place it in the `lib/lwjgl/` folder
3. Rename it to `minecraft-1.2.5.jar`

### JAR File Location
- **Path**: `lib/lwjgl/minecraft-1.2.5.jar`
- **Size**: Approximately 4-5 MB
- **Source**: Official Mojang servers (via download script)

## Mod Support

To add mods to your Minecraft installation:

1. **Create mods folder** (if it doesn't exist):
   ```bash
   mkdir -p mods
   ```

2. **Add mod JAR files**:
   Place any Forge-compatible mod JAR files for Minecraft 1.2.5 in the `/mods` folder.

3. **Restart the game**:
   The mods will be loaded automatically when you start Minecraft.

### Supported Mods
- Any Forge-compatible mods for Minecraft 1.2.5
- Mods should be in JAR format
- Place them directly in the `/mods` folder

## How it Works

This project uses [CheerpJ](https://cheerpj.com), a Java-to-JavaScript compiler that allows Java applications to run in the browser. The implementation:

1. **Loads Local JAR**: Fetches the Minecraft 1.2.5 client JAR from the local `lib/` folder
2. **Virtual Filesystem**: Writes the JAR into CheerpJ's virtual filesystem at `/files/client_1.2.5.jar`
3. **Initializes CheerpJ**: Sets up the Java runtime environment in the browser
4. **Runs Minecraft**: Executes the unmodified Minecraft client JAR with LWJGL libraries
5. **Loads Mods**: Automatically loads any mods placed in the `/mods` folder

### Technical Details

The application uses CheerpJ's virtual filesystem to store and run the JAR file. The LWJGL libraries are stored alongside the client in the CheerpJ filesystem. The process is:

1. The JAR file is loaded from `/lib/lwjgl/minecraft-1.2.5.jar` on the web server
2. It's written to `/files/client_1.2.5.jar` in CheerpJ's virtual filesystem
3. CheerpJ runs Minecraft with the classpath: `/files/client_1.2.5.jar:/files/lwjgl-2.9.3.jar:/files/lwjgl_util-2.9.3.jar`

## File Structure

```
minecraft-1.2.5/
├── index.html              # Main application
├── download-minecraft.sh   # Script to download JAR file
├── lib/                    # JAR files directory
│   └── lwjgl/minecraft-1.2.5.jar # Minecraft client JAR (download required)
├── mods/                   # Place your mod JAR files here
│   └── README.md          # Instructions for adding mods
├── package.json           # Node.js dependencies
└── README.md             # This file
```

## Troubleshooting

### Common Issues

**JAR File Not Found**
- Run `./download-minecraft.sh` to download the JAR file
- Ensure the file exists at `lib/lwjgl/minecraft-1.2.5.jar`
- Check file permissions

**CheerpJ Loading Fails**
- Ensure you have a modern browser with WebGL support
- Check browser console for error messages
- Try refreshing the page

**Game Won't Start**
- Check that you've accepted the Minecraft EULA
- Verify the JAR file exists and is not corrupted
- Check browser console for error messages
- Ensure JavaScript is enabled in your browser

**404 Errors for LWJGL**
- This is normal - LWJGL libraries are provided internally by CheerpJ
- The game should still work despite these errors in the network tab

**Mods Not Loading**
- Verify mods are compatible with Minecraft 1.2.5
- Ensure mods are in JAR format
- Check that mods are placed directly in the `/mods` folder

**Performance Issues**
- Close other browser tabs
- Use a more powerful computer
- Ensure you have sufficient RAM available
- Try using Chrome or Firefox for best performance

## Development

### Local Development
```bash
npm run dev          # Start development server
npm run build        # Build for production (not implemented yet)
```

### Adding Features
This is a minimal implementation. To add features:
- Modify `index.html` for UI changes
- Add mod loading logic if needed
- Customize CheerpJ configuration in the JavaScript

## Legal Notice

This project is for educational purposes. You must:
- Own a legal copy of Minecraft 1.2.5
- Comply with the Minecraft EULA
- Respect Mojang's intellectual property rights

## License

This project is licensed under the MIT License.

## Acknowledgments

- [CheerpJ](https://cheerpj.com) for making Java applications run in the browser
- [Browsercraft](https://github.com/leaningtech/browsercraft) for the original inspiration
- Mojang for Minecraft (this is not an official product) 