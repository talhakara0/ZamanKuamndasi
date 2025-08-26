# Zaman Kumandası - Premium Satın Alma Sistemi

## Özellikler

### ✨ Premium Paketler
- **Aylık Premium**: ₺29,99 - Aylık erişim
- **Yıllık Premium**: ₺89,99 - %60 tasarruf
- **Yaşam Boyu Premium**: ₺249,99 - Tek seferlik ödeme

### 🎯 Premium Avantajları
- ✅ Reklamların tamamen kaldırılması
- ✅ Sınırsız çocuk hesabı bağlama
- ✅ Gelişmiş uygulama kontrolü (3+ uygulama limiti)
- ✅ Gelecek özellikler için erken erişim

## Kullanım

### Satın Alma Ekranına Erişim

#### Parent Dashboard'dan:
1. Ebeveyn panelinde "Premium'a Geç" butonuna tıklayın
2. Mevcut premium paketleri görüntüleyin
3. İstediğiniz paketi seçin

#### Child Dashboard'dan:
1. Çocuk panelinde "Premium'a Geç" butonuna tıklayın
2. Ebeveyn onayı ile satın alma işlemi

### Satın Alma Süreci

1. **Paket Seçimi**: 3 farklı premium paketten birini seçin
2. **Google Play Ödeme**: Güvenli Google Play Store üzerinden ödeme
3. **Anında Aktivasyon**: Satın alma sonrası otomatik premium aktivasyonu
4. **Reklam Kaldırma**: Uygulamadaki tüm reklamlar anında devre dışı

### Premium Durum Kontrolü

Uygulama otomatik olarak:
- Google Play satın alımlarını kontrol eder
- Premium durumunu senkronize eder
- Reklam gösterimini yönetir
- Premium özelliklerine erişimi açar

## Teknik Detaylar

### Kullanılan Teknolojiler
- **Google Play Billing Client 7.0.0**: En güncel ödeme API'si
- **Kotlin Coroutines**: Asenkron işlemler için
- **Hilt Dependency Injection**: Bağımlılık yönetimi
- **MVVM Architecture**: Temiz mimari yaklaşımı

### Güvenlik
- ✅ Server-side receipt verification (gelecek güncelleme)
- ✅ Purchase token validation
- ✅ Anti-fraud koruma
- ✅ Secure payment processing via Google Play

### Test Ortamı
- Google Play Console test kullanıcıları
- Test product ID'leri
- Sandbox payment processing
- Real-time premium activation testing

## Dosya Yapısı

```
app/src/main/java/com/example/zamankumandasi/
├── billing/
│   └── BillingManager.kt              # Google Play Billing yönetimi
├── data/model/
│   └── PurchaseProduct.kt             # Satın alma ürün modeli
├── ui/purchase/
│   └── PurchaseFragment.kt            # Satın alma ekranı
├── ui/adapter/
│   └── PurchaseProductAdapter.kt      # Ürün listesi adapter'ı
├── ui/viewmodel/
│   └── PurchaseViewModel.kt           # Satın alma ViewModel'i
└── di/
    └── BillingModule.kt               # Hilt modülü
```

## Gelecek Geliştirmeler

### Planlanan Özellikler
- 🔄 Server-side receipt verification
- 📊 Temel kullanım istatistikleri
- 🎁 Promotional offers
- 👨‍👩‍👧‍👦 Family sharing options
- 📱 Multi-device sync
- 🌍 Localized pricing

### Optimizasyonlar
- ⚡ Faster billing initialization
- 🔄 Better error handling
- 📱 Improved UI/UX
- 🌐 Offline purchase restoration

## Destek

Premium ile ilgili sorunlar için:
- 📧 Email: support@zamankumandasi.com
- 🔄 In-app "Satın Alımları Geri Yükle" özelliği
- 📱 Google Play Store support

---

**Zaman Kumandası Premium** - Aileler için güvenli dijital kontrol
