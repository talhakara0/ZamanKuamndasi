# Zaman Kumandası Web Sitesi

Bu klasör `zamankumandasi.com` domain'i için hazırlanmış statik web sitesi dosyalarını içerir.

## 📁 Dosya Yapısı

```
website/
├── index.html          # Ana sayfa
├── privacy.html        # Gizlilik politikası (/privacy)
├── support.html        # Destek sayfası (/support)  
├── contact.html        # İletişim sayfası (/contact)
├── .htaccess          # Apache server konfigürasyonu
└── README.md          # Bu dosya
```

## 🚀 Kurulum Talimatları

### Seçenek 1: Shared Hosting (Önerilen - En Kolay)

1. **Hosting sağlayıcınıza giriş yapın** (Turhost, Natro, Hostinger, vs.)
2. **cPanel veya File Manager'ı açın**
3. **public_html klasörüne gidin**
4. **Bu klasördeki tüm dosyaları yükleyin:**
   ```
   index.html
   privacy.html
   support.html
   contact.html
   .htaccess
   ```
5. **Domain'i zamankumandasi.com'a yönlendirin**

### Seçenek 2: GitHub Pages (Ücretsiz)

1. **GitHub'da yeni repository oluşturun**
2. **Bu dosyaları repository'e yükleyin**
3. **Settings > Pages'den GitHub Pages'i aktifleştirin**
4. **Custom domain olarak zamankumandasi.com'u ekleyin**

### Seçenek 3: Netlify (Ücretsiz + Kolay)

1. **netlify.com'a kayıt olun**
2. **"New site from Git" veya "Deploy manually" seçin**
3. **Bu klasörü drag & drop ile yükleyin**
4. **Site settings'den custom domain ekleyin**

### Seçenek 4: Vercel (Ücretsiz + Hızlı)

1. **vercel.com'a kayıt olun**
2. **"New Project" tıklayın**
3. **Bu klasörü yükleyin**
4. **Domain settings'den zamankumandasi.com ekleyin**

## 🔧 Konfigürasyon

### Domain Yönlendirme

Domain sağlayıcınızın (GoDaddy, Namecheap, vs.) DNS ayarlarından:

```
A Record: @ → Hosting IP adresiniz
A Record: www → Hosting IP adresiniz
```

### SSL Sertifikası

- Çoğu hosting sağlayıcısı ücretsiz Let's Encrypt SSL sağlar
- GitHub Pages, Netlify, Vercel otomatik SSL verir

## 📋 Kontrol Listesi

### Yükleme Sonrası Kontrol Edin:

- [ ] `https://zamankumandasi.com` ana sayfayı açıyor
- [ ] `https://zamankumandasi.com/privacy` gizlilik politikasını gösteriyor
- [ ] `https://zamankumandasi.com/support` destek sayfasını açıyor  
- [ ] `https://zamankumandasi.com/contact` iletişim sayfasını gösteriyor
- [ ] Mobil cihazlarda düzgün görünüyor
- [ ] SSL sertifikası çalışıyor (yeşil kilit ikonu)

### Android Manifest Güncellemesi

Web sitesi yayındayken `app/src/main/AndroidManifest.xml` dosyasında:

```xml
<meta-data
    android:name="android.support.PRIVACY_POLICY_URL"
    android:value="https://zamankumandasi.com/privacy" />

<meta-data
    android:name="android.support.SUPPORT_EMAIL"
    android:value="support@zamankumandasi.com" />
```

URL'lerin çalıştığından emin olun!

## 🎨 Tasarım Özellikleri

- **📱 Responsive:** Tüm cihazlarda çalışır
- **🎨 Modern:** Gradient arka plan ve cam efekti
- **⚡ Hızlı:** Optimize edilmiş CSS, sıkıştırma
- **🔒 Güvenli:** Security headers, HTTPS yönlendirme
- **♿ Erişilebilir:** Semantic HTML, iyi kontrast

## 📞 Destek

Web sitesi kurulumu ile ilgili sorunlar için:
- 📧 `support@zamankumandasi.com`
- 📱 Uygulama içi destek

## 🔄 Güncelleme

Yeni özellikler veya değişiklikler için:
1. İlgili HTML dosyasını güncelleyin
2. Hosting'e yeniden yükleyin
3. Cache'i temizleyin (Ctrl+F5)

---

**🎉 Bu dosyaları yükledikten sonra `https://zamankumandasi.com/privacy` URL'niz aktif olacak!**
