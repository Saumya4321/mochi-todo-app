# Mochi Todo for Android

An Android app that opens your synced Mochi Todo full-screen, with no Chrome bar.
Every file here is a plain file. Nothing goes in folders.

## Get the APK

1. On github.com, create a new **private** repository (e.g. `mochi-todo-app`).
2. **Add file → Upload files**, select all of these files at once, then **Commit changes**.
3. Open the **Actions** tab → **set up a workflow yourself**. Delete what's in the editor,
   paste in everything from `build-apk.yml`, then **Commit changes**.
4. The build starts by itself and takes about 3–5 minutes. When it shows a green tick, open the run
   and download **MochiTodo-apk** under **Artifacts**. Unzip it to get `MochiTodo.apk`.
5. Open the APK on your phone and allow installing it when Android asks.

## First launch

Sign in to Claude with **Continue with Google** inside the app. It's remembered after that.
If Google refuses the in-app sign-in, use **Continue with email** with the same Gmail
address. Before tapping the email link, go to: long-press the app icon → **App info** →
**Open by default** → **Add link** → tick `claude.ai`. The link then opens in Mochi Todo.

Keep the repository private: `mochi.keystore` lets new versions install over the old one.
