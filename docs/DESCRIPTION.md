# 🌨️ Serene Seasons Better Winter

Bring your winter landscapes to life with **cleaner tree silhouettes** and a **stronger seasonal atmosphere**.

**Serene Seasons Better Winter** is a Forge **1.20.1 addon** that makes **deciduous tree leaves visually disappear** during **late autumn and winter**, creating a natural dormant-season look while **keeping conifer and jungle foliage intact**.

---

## 🎥 Showcase

### Cinematic Setup (Recommended)

**Embeddium + Dynamic Trees + Complementary Unbound + Serene Seasons Better Winter**

![Main Showcase GIF - Embeddium + Dynamic Trees + Complementary Unbound](https://media.forgecdn.net/attachments/description/null/description_e7124ef7-bffc-4255-bbd7-dcc52da70321.gif)

---

### Vanilla Renderer (Without Embeddium)

![Vanilla Without Embeddium](https://media.forgecdn.net/attachments/description/null/description_079c5f1e-2604-4258-9eeb-a78ab345ae8b.png)

> **Note:** Without Embeddium, some residual canopy shadowing may still occur in very dense or tall foliage.

---

### Vanilla + Embeddium

![Vanilla With Embeddium](https://media.forgecdn.net/attachments/description/null/description_e9242389-b774-475b-bd40-82f2334c9db3.png)

---

## 📦 Requirements

- **Serene Seasons** *(required)*

---

## ⭐ Highly Recommended

- **Embeddium**  
  Best visual consistency, especially for lighting and shadow behavior.
- **Dynamic Trees**  
  Excellent visual synergy with branch-heavy, leafless winter canopies.

---

## ✨ Features

- Hides **deciduous leaves** in configured leaf-drop sub-seasons *(default: late autumn + winter)*
- Keeps **conifer and jungle leaves** visible
- Removes **floating snow layers** above hidden leaves
- Hides **small hanging blocks** attached below hidden leaves *(for example apples and cocoons)*
- Optional **server-side pass-through** for hidden seasonal blocks
- Optional **hidden-block lighting behavior** adjustments
- Optional **season chat broadcast** on leaf-drop and leaf-return transitions
- Optional **Distant Horizons integration** for LOD leaf hiding and refresh *(experimental, client-side)*

---

## 🧪 Distant Horizons Integration (Experimental)

Distant Horizons support is available as an **experimental client feature**.

In `serene_better_winter-client.toml`:
- `enable_experimental_dh_integration = true`
- `enable_dh_lod_leaf_hiding = true`
- `dh_auto_refresh_on_season_toggle = true`

**Important:**
- This integration is **disabled by default**.
- On some modpacks/setups, LOD refresh behavior may be inconsistent.
- If needed, disable the experimental DH integration and keep the base mod behavior.

---

## ℹ️ Version Info

- **Minecraft:** 1.20.1
- **Forge target:** 47.4.10 *(runtime-compatible range)*
- **Mod ID:** `serene_better_winter`

---

## 🐞 Bug Reports and Issues

If you encounter bugs, visual inconsistencies, or compatibility issues, please report them on GitHub:

**Issues tracker:**  
https://github.com/Maeiro/Serene-Seasons-Better-Winter/issues

When reporting an issue, including screenshots, logs, and your full mod list is highly appreciated.

---

## 📜 Modpack and Code Usage

This mod is released under the **GNU GPL v3.0**.

You are free to:
- Use the mod in **public or private modpacks**
- Modify, fork, or reuse the **source code**
- Include parts of the code in your own projects
- Distribute modified or unmodified versions

As required by GPLv3, redistributions and derivative works must keep the same license terms.

---

## 📄 License

This project is licensed under the **GNU GPL v3.0**.  
See the `LICENSE` file for the full license text.
