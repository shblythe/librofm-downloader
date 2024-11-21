# Libro.fm Audiobook Downloader

Small tool for checking your [libro.fm](https://libro.fm) library and downloading new books.

The tool is set to recheck the library every hour and download new books. Books will be skipped if the `Author Name/Book Name` folder already exists, or if the book has been downloaded previously.

```
services:
  librofm-downloader:
    image: ghcr.io/burntcookie90/librofm-downloader:latest
    volumes:
      - /mnt/runtime/appdata/librofm-downloader:/data
      - /mnt/user/media/audiobooks:/media
    environment:
      - LIBRO_FM_USERNAME=<>
      - LIBRO_FM_PASSWORD=<>
      # optional
      - DATA_DIR=<>  // data dir in the container, default is /data. Make sure you bind this correctly
      - MEDIA_DIR=<> // media dir in the container, default is /media. Make sure you bind this correctly
      # extra optional: setting these enables them, dont add them if you dont want them.
      - DRY_RUN=true 
      - VERBOSE=true
```
