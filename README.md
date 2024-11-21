# Libro.fm Audiobook Downloader

```
services:
  librofm-downloader:
    image: ghcr.io/burntcookie90/librofm-downloader:main
    volumes:
      - /mnt/runtime/appdata/librofm-downloader:/data
      - /mnt/user/media/audiobooks:/media
    environment:
      - LIBRO_FM_USERNAME=<>
      - LIBRO_FM_PASSWORD=<>
```