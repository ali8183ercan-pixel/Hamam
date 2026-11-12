# Roach Rush — Android APK ve İndirme Sitesi Rehberi

Bu depo hem **Roach Rush** Android oyununu hem de telefonunuzdan tek dokunuşla APK indirebileceğiniz web vitrini (`docs/index.html`) dosyasını içerir.

## 1. GitHub Actions ile APK Üretme
1. AI Studio menüsünden **GitHub'a Aktar (Push to GitHub)** ile güncel dosyaları deponuza gönderin.
2. GitHub deponuzda **Actions** sekmesini açıp `Build Roach Rush APK` akışının yeşil tik almasını bekleyin (~2–3 dakika).
3. İşlem bitince APK'yı şu 2 yerden birinden indirebilirsiniz:
   - **Releases** sekmesi (`https://github.com/ali8183ercan-pixel/Hamam/releases`) -> **`RoachRush-debug.apk`**
   - **Actions** sekmesi -> Son çalıştırmanın içindeki **Artifacts** -> **`RoachRush-debug-apk`**

> **Not (Eğer Releases veya gh-pages izin hatası görürseniz):** GitHub deponuzda **Settings > Actions > General > Workflow permissions** bölümünden **Read and write permissions** seçeneğini işaretleyip Save'e basın.

## 2. APK İndirme Web Sitesini Açma (GitHub Pages)
Derleme tamamlandığında iş akışı `gh-pages` dalına hem `index.html` sitesini hem de `RoachRush-debug.apk` dosyasını otomatik koyar:
1. GitHub deponuzda **Settings > Pages** menüsüne girin.
2. **Build and deployment > Source** kısmını **Deploy from a branch** yapın.
3. Branch olarak **`gh-pages`** (klasör: `/ (root)`) seçip **Save** deyin.
4. ~1 dakika sonra siteniz **`https://ali8183ercan-pixel.github.io/Hamam/`** adresinde yayına girer ve telefonunuzdan tek dokunuşla APK indirebilirsiniz.
