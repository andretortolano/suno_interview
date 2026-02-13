# Suno Interview App

A modern Android application for streaming and enjoying music from Suno, built with Jetpack Compose, Media3, and Clean Architecture.

## 🎬 Project Overview

This app provides a seamless music and video playback experience with a focus on smooth transitions and modern UI components.

[![App Demo](docs/app_demo.mp4)](https://github.com/user-attachments/assets/ba7c9d1f-e730-49f6-890c-cc7165c9a6f9)

## ✨ Features

### 🎵 Song List
- Browse a curated collection of Suno songs.
- Paginated loading for a smooth scrolling experience using the Paging 3 library.
- High-quality artwork display for each track.

### 📱 Vertical Player
- Immersive full-screen vertical playback experience (similar to modern short-form video platforms).
- Support for both high-quality audio and video playback.
- Interactive playback controls (Seek, Play/Pause, Fast Forward/Rewind).
- Smooth transitions and vertical paging between tracks.
- Uses `TextureView` for perfect synchronization with Compose animations.

### 🎼 Playback & Notification Service
- Background playback support powered by **Media3 Exoplayer** and **MediaSession**.
- Persistent media notification with playback controls and metadata.
- Seamless synchronization between the app UI and system media controls.

### 🏎️ Home Mini Player
- Persistent mini-player at the bottom of the song list for quick access to current playback.
- Real-time synchronization of playback status (Play/Pause).
- Swipe-to-dismiss functionality for a clean user experience.
- One-tap navigation back to the full-screen vertical player.

## 🛠️ Tech Stack

- **UI:** Jetpack Compose with Material 3.
- **Media:** Android Media3 (ExoPlayer, Session).
- **Architecture:** MVVM with Clean Architecture principles.
- **Dependency Injection:** Dagger Hilt.
- **Image Loading:** Coil.
- **Networking:** Retrofit / OkHttp.
- **Pagination:** Paging 3.

## 📁 Project Structure

- `feature/home`: Contains the song list and mini-player components.
- `feature/player`: Contains the vertical full-screen player logic.
- `ui/service`: Houses the `PlaybackService` for background media handling.
- `domain`: Contains models and repository interfaces.
- `data`: Implementation of data sources and repositories.
