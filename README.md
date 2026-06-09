<div align="center">

[![Available on Modrinth](https://raw.githubusercontent.com/intergrav/devins-badges/c7fd18efdadd1c3f12ae56b49afd834640d2d797/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/enhanced-chat)
[![Chat with us on Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/FCPP2WPZ3U)
[![Requires Fabric API](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_vector.svg)](https://modrinth.com/mod/fabric-api)

</div>
<div align="center">

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/enhanced-chat?color=00AF5C&label=downloads&logo=modrinth&style=flat-square)](https://modrinth.com/mod/enhanced-chat)
[![Join Fluxer](https://img.shields.io/badge/Join-Fluxer-5865F2?style=flat-square)](https://fluxer.gg/3jJy9cp6)

# Enhanced Chat

**A quality-of-life chat upgrade for Hypixel SkyBlock and general use. Adds better formatting, search, filtering, and other small improvements.**
</div>

## Features

### Compact Duplicate Messages
Merges repeated chat messages into a single line with an occurrence counter (×N). Choose between consecutive-only, time-windowed, or unlimited compaction. Optionally skip messages with click events so interactable links stay intact.

### Centered Hypixel Text
Detects space-padded centered messages from Hypixel and actually centers them in the chat window — no more lopsided banners or profile views.

### Smooth Separators
Replaces dash, block, and line separator characters with clean horizontal pixel lines. Supports both full separators and separators with centered text.

### Chat Tabs
Adds clickable channel tabs above the chat input — **All, User, Party, Guild, PM, and Co-op** — so you can filter chat by message type. Separator lines automatically group with their surrounding tick-block so banner borders don't disappear when switching tabs.

### Chat Context Menu
Right-click any chat message to open a menu with options to **Copy Text**, **Copy Message Body**, **Copy with &Codes**, or **Delete** the message. Prefer instant copying? Enable **Right-Click to Copy** in config to skip the menu entirely.

### Chat Search
Press **Ctrl+F** while chat is open to search through your chat history. Multi-word AND filtering means every token must match. The match count is shown live, and you can pin the search bar to always stay visible.

### Extended Chat History
Increases the vanilla 100-message chat history limit to a configurable value — up to **2,048 messages** — so you never lose important chat context.

### Chat Animation
Smooth slide-up animation when new messages arrive, plus a subtle cubic ease-out on the chat input bar when opening chat. Both the effect and duration are configurable.

---

## Configuration

Open the config via **Mod Menu → Enhanced Chat → Config**. Enhanced Chat uses MidnightLib, so all options are editable in-game with live saving.

| Option | Description |
|--------|-------------|
| Extended Chat History | Enable and set the history size (100–2048). |
| Compact Duplicate Messages | Enable compaction, choose mode, and set time window. |
| Center Hypixel Text | Properly center space-padded Hypixel messages. |
| Smooth Separators | Replace separator characters with clean lines. |
| Enable Chat Tabs | Show channel filter tabs above chat input. |
| Enable Chat Context Menu | Right-click menu for copy/delete actions. |
| Right-Click to Copy | Instantly copy raw text without opening the menu. |
| Enable Chat Search | Toggle search with Ctrl+F. |
| Always Show Search Bar | Keep the search field permanently visible. |
| Enable Chat Animation | Smooth slide-up for messages and input bar. |
| Animation Duration | How long the animation lasts (50–500 ms). |

---

## Installation

1. Install Minecraft with **Fabric Loader** for 26.1+.
2. Download the latest `.jar` from [Modrinth](https://modrinth.com/mod/enhanced-chat).
3. Place **Fabric API** in your `mods` folder. MidnightLib and HM-API are bundled inside the Enhanced Chat jar — no separate downloads needed.
4. Launch the game. The mod activates automatically on the client.

## Support & Community

Found a bug or have a feature request? Come say hi.

[![Chat with us on Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/FCPP2WPZ3U)
[![Join Fluxer](https://img.shields.io/badge/Join-Fluxer-5865F2?style=flat-square)](https://fluxer.gg/3jJy9cp6)

## Support the Project

If you'd like to support continued development, you can do so on **Ko-fi** — every contribution is appreciated.

[**Support on Ko-fi →**](https://ko-fi.com/kdgaming1)

---

<div align="center">

**Made with love for the SkyBlock community.**

</div>
