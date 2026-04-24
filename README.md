# DrupalTracker

A mobile companion app for tracking Drupal.org projects and issues. Search, bookmark, get notified about updates, and understand complex issue threads with AI-powered summaries.

---

## Features

- **Search** projects and issues on Drupal.org
- **Star** projects and issues to keep track of them
- **Notifications** when starred projects or issues are updated
- **AI summaries** of issue threads powered by Google Gemini
- **In-app browser** to read full issue pages on drupal.org

---

## Screens

### Search

The default screen when you open the app. Use the toggle at the top to switch between two search modes:

- **By Project** — search by machine name (e.g. `views`) or title keywords
- **By Issue** — search by title keywords or a direct node ID (NID)

Tap any project to browse its issues. Tap any issue to open it in the built-in browser. Use the star icon on any project or issue to bookmark it.

### Starred

Shows all your bookmarked items in two sub-tabs:

- **Projects** — starred projects; tap to browse their issues
- **Issues** — starred issues with status, priority, and last-updated info

Tap the yellow star to remove a bookmark.

### Notifications

Tap the bell icon in the top bar to view your notification history. Each entry shows a title, message, and timestamp. Tap **Open project** or **Open issue** to navigate directly to that item.

The bell icon shows a badge with the count of unread notifications (up to 99+).

### Settings

Tap the gear icon in the top bar to configure the app.

---

## Setup

### Gemini API Key (for AI summaries)

1. Go to **Settings** and find the **Gemini API Key** section.
2. Visit [aistudio.google.com](https://aistudio.google.com) to get a free key (1,500 requests/day at no cost).
3. Paste the key into the field and tap **Save**.

Once saved, a **Summary** button (wand icon) appears on every issue card.

### Notifications

1. Go to **Settings** and turn on **Enable notifications**.
2. Configure separately for projects and issues:

**Starred Projects:**
- Toggle **Notify for starred projects** on/off
- Choose notification type:
  - *Every update* — one notification per change
  - *Digest* — grouped notifications on a schedule (Hourly / Daily / Weekly)

**Starred Issues:**
- Toggle **Notify for starred issues (digest)** on/off
- Updates are always delivered as grouped digest notifications

---

## Using the App

### Finding a Project

1. Open the **Search** tab.
2. Select **By Project**.
3. Type a project machine name (e.g. `webform`) or keyword and tap **Search**.
4. Tap a result to browse its issues, or tap the star to bookmark it.

### Finding an Issue

1. Open the **Search** tab.
2. Select **By Issue**.
3. Type a keyword or paste a node ID (NID) and tap **Search**.
4. Tap an issue to open it in the browser, or star it to track it.

### Filtering Issues Within a Project

When viewing a project's issues, use the **keyword filter** field at the top and tap **Search** to narrow results.

### Getting an AI Summary

Tap the **Summary** button (wand icon) on any issue card. A panel slides up showing:

- **Problem** — what the issue is about
- **Discussion** — key points from the comments
- **Next Steps** — what needs to happen or what's blocking progress
- **How can I help** — specific ways you can contribute

Summaries are cached locally and updated when new comments appear. Requires a Gemini API key (see Setup above).

---

## Issue Status & Priority Reference

**Status:**
Active · Fixed · Needs review · Needs work · RTBC · Patch (to be ported) · Postponed · Postponed (needs info) · Closed (duplicate) · Closed (won't fix) · Closed (works as designed) · Closed (fixed) · Closed (outdated) · Closed (cannot reproduce)

**Priority:**
🔴 Critical · 🟠 Major · 🔵 Normal · ⚪ Minor
