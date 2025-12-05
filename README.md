# PrintMe - Home Photo Print Preparation App

> ⚠️ **AI-Generated Code Notice**: This project was created fully by AI (GitHub Copilot with Claude). I have no skill or experience in the technology used here.

A simple Android application for preparing photos for home printing with customizable layouts and instant-camera style margins.

## Features

- **Photo Selection** - Select multiple photos from your device
- **Layout Options** - Arrange 2, 3, or 4 photos per page
- **Instant Camera Margins** - Polaroid/Instax style margins with larger bottom border
- **Print Preview** - See exactly how your photos will print
- **Direct Printing** - Send to any Android-compatible printer

## Screenshots

*(Coming soon)*

## Requirements

- Android 8.0 (API 26) or higher
- Any printer compatible with Android Print Service

## Installation

### From APK
1. Download the latest APK from [Releases](releases)
2. Enable "Install from unknown sources" if prompted
3. Install the APK

### Build from Source

**Prerequisites:**
- Android Studio (recommended) or Android SDK
- JDK 17 or higher

**Option A: Using Android Studio (Recommended)**
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `print_me` folder
4. Wait for Gradle sync to complete
5. Click "Run" or press Shift+F10

**Option B: Command Line**
```bash
# Clone the repository
git clone https://github.com/yourusername/print_me.git
cd print_me

# Set Android SDK path (if not using ANDROID_HOME env variable)
cp local.properties.template local.properties
# Edit local.properties and set sdk.dir to your SDK path

# Build debug APK
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk

# Install on connected device (optional)
./gradlew installDebug
```

## Usage

### 1. Select Photos
Tap **"Add Photos"** and select the photos you want to print. You can select multiple photos at once.

### 2. Choose Layout
Select how many photos per page:
- **2 photos** - Stacked vertically
- **3 photos** - One on top, two on bottom
- **4 photos** - 2×2 grid

### 3. Configure Margins
Choose a margin style:
- **Polaroid** - Classic instant camera look (larger bottom margin)
- **Instax** - Fujifilm Instax style
- **Equal** - Same margin on all sides
- **None** - No margins

### 4. Preview & Print
- Swipe to preview all pages
- Tap **"Print"** to open Android print dialog
- Select your printer and print settings
- Print!

## Layout Examples

```
2 Photos (Stacked):
┌──────────────────────┐
│       Photo 1        │
└──────────────────────┘
┌──────────────────────┐
│       Photo 2        │
└──────────────────────┘

4 Photos Grid (2×2):
┌──────────┐ ┌──────────┐
│  Photo 1 │ │  Photo 2 │
└──────────┘ └──────────┘
┌──────────┐ ┌──────────┐
│  Photo 3 │ │  Photo 4 │
└──────────┘ └──────────┘
```

## Margin Styles

**Polaroid Style:**
```
┌─────────────────────┐
│ ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒ │  ← 8mm top
│ ▒┌───────────────┐▒ │
│ ▒│     PHOTO     │▒ │  ← 8mm sides
│ ▒└───────────────┘▒ │
│ ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒ │
│ ▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒ │  ← 20mm bottom
└─────────────────────┘
```

## Supported Paper Sizes

- A4 (210 × 297 mm)
- Letter (8.5 × 11")
- 4×6" Photo Paper
- 5×7" Photo Paper
- A5 (148 × 210 mm)

## Permissions

The app requires the following permissions:
- **Read Media Images** (Android 13+) / **Read External Storage** (Android 12-) - To access your photos

## Troubleshooting

### Photos not loading
- Ensure you granted storage permission
- Try restarting the app
- Check if the photo file is corrupted

### Can't find printer
- Make sure printer is on and connected to the same network
- Install your printer's Android app if available
- Try "Save as PDF" to test

### Print quality is poor
- Use higher resolution photos (minimum 1200×1800 pixels for 4×6")
- The app will warn you if photo resolution is too low

## Development

For implementation details, see [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## License

MIT License - See [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please read the [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for development details.

---

*Made with ❤️ and AI assistance*
