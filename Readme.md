# ❤️ Hearts Plugin for Paper

A lightweight and visually enhanced Minecraft plugin that adds **smooth heart-based HP indicators** and integrates with **Vault** to display player balances directly under their health bar.

---

## ✨ Features

* ❤️ **Smooth Health Transitions** – HP bar animates smoothly when taking or healing damage
* 💰 **Vault Integration** – Shows player balance under the hearts
* 🌈 **Custom Color Formatting** – Beautifully styled heart colors and text gradients
* ⚙️ **Optimized for Paper 1.21+**
* 💬 **Lightweight & Lag-Free**
* 📱 **Bedrock Support (Geyser/Floodgate)** – Bedrock players see hearts through Action Bar
* 👁️ **Floating ArmorStand Display** – Hearts appear above all players and mobs
* 🔄 **Automatic 0.5s Updates** – Continually refreshed without lag
* 🎯 **Works Without Floodgate/Vault** – Features enable dynamically based on installed plugins

---

## 🧩 Requirements

* **Minecraft:** Paper 1.21+
* **Vault Plugin** (optional)
* **Economy Plugin** (EssentialsX, CMI, etc.)
* **Geyser + Floodgate** (optional for Bedrock actionbar)
* **Java 21 or higher**

---

## 🚀 Installation

1. Download the latest `Hearts.jar` from [Releases](../../releases)
2. Drop it into your `plugins` folder
3. Restart your server
4. Enjoy smooth hearts, balance display, and Bedrock support!

---

## 🧠 Configuration

The plugin currently does **not** use a config file — all features are automatic.

Future versions may include options like:

```yaml
enable-actionbar: true
show-float-hearts: true
smooth-transition: true
vault-display: true
update-interval: 10 # in ticks
color: "&c"
range: 12 # action bar send radius for bedrock players
show-mobs: true
```

---

## 🔮 Upcoming Updates

* `/hearts reload` command
* Toggle hearts per-player
* Configurable heart styles (icons/colors)
* Distance-based optimizations
* Option to disable mob heart rendering
* Custom text formatting for balance display
* Performance profiler for armor stands
* RGB/Gradient text support for Java players

---

## 📸 Showcase (coming soon)

Screenshots and previews will be added in future updates.

---

## 🧾 License

MIT License – free to modify & use.

---

> Made for a smooth, modern Minecraft experience ❤️

## 🆕 Recent Updates

### ✔ Bedrock Action Bar Support

Bedrock players (Floodgate) now receive heart bars through action bar since they can’t see floating text properly.

### ✔ Custom Death Messages

Now the plugin shows:

* Who killed the player (mob/player)
* Exact reason of death (fall, lava, fire, explosion, etc.)
* Remaining health display like: `💔 11.2/20.0`

Example:

```
Player Pro215 was slain by Zombie 💔 11.2/20.0
```

### ✔ Floodgate + Geyser Safe Integration

No crashes if Floodgate is not installed. Bedrock detection is safe.

### ✔ Improved Floating Heart System

* Auto-spawning invisible armor stands
* Smooth updates every 10 ticks
* Works for all mobs + players

### ✔ Vault Economy Enhancements

Shows balance below heart bar for Java players.

### ✔ Fully Compatible with Paper 1.21+

No deprecated API. Attribute system updated.

---

If you want, I can also add a **preview image**, **plugin.yml**, or **config.yml** section.
