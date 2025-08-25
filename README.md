# Zaman Kumandası - Android Uygulaması

Ebeveynlerin çocuklarının telefon kullanımını kontrol etmelerini sağlayan Android uygulaması.

## Özellikler

### 🔐 Kimlik Doğrulama
- Firebase Authentication ile e-posta/şifre kayıt ve giriş
- Ebeveyn ve çocuk hesap türleri
- Eşleştirme kodu ile ebeveyn-çocuk bağlantısı
- **Gelişmiş logout sistemi** - Güvenli çıkış, session yönetimi, navigation temizleme
- **Offline Mode Desteği** - İnternet olmasa bile uygulama çalışır
- **Manuel Çıkış** - Otomatik çıkış yok, kullanıcı kontrollü session yönetimi

### 📱 Uygulama Takibi
- UsageStatsManager ile uygulama kullanım süresi takibi
- Günlük süre sınırları
- Gerçek zamanlı kullanım izleme

### 🛡️ Kontrol Mekanizmaları
- Süre aşımında uygulama engelleme
- Foreground Service ile sürekli takip
- Usage Access izni yönetimi

### 📊 Ebeveyn Paneli
- Bağlı çocukları görüntüleme
- Uygulama süre sınırları ayarlama
- Kullanım raporları

## Teknik Detaylar

### Mimari
- **MVVM (Model-View-ViewModel)** mimarisi
- **Hilt** dependency injection
- **Firebase** backend (Authentication, Firestore)
- **Navigation Component** ile ekran geçişleri
- **Coroutines** ile asenkron işlemler

### Kullanılan Teknolojiler
- Kotlin
- Android Jetpack (ViewModel, LiveData, Navigation)
- Firebase (Auth, Firestore)
- Hilt (Dependency Injection)
- Material Design Components
- UsageStatsManager API

## Kurulum

### Gereksinimler
- Android Studio Arctic Fox veya üzeri
- Android SDK 24+
- Firebase projesi

### Adımlar

1. **Firebase Projesi Oluşturma**
   ```bash
   # Firebase Console'da yeni proje oluşturun
   # Android uygulaması ekleyin
   # google-services.json dosyasını app/ klasörüne yerleştirin
   ```

2. **Projeyi Klonlama**
   ```bash
   git clone <repository-url>
   cd ZamanKumandasi
   ```

3. **Bağımlılıkları Senkronize Etme**
   ```bash
   # Android Studio'da "Sync Project with Gradle Files" yapın
   ```

4. **Uygulamayı Çalıştırma**
   ```bash
   # Emülatör veya fiziksel cihazda çalıştırın
   ```

## Kullanım

### Ebeveyn Hesabı
1. Uygulamayı açın ve "Kayıt Ol" seçin
2. Ebeveyn hesap türünü seçin
3. E-posta ve şifre ile kayıt olun
4. Size verilen eşleştirme kodunu not edin
5. Çocuk cihazında bu kodu kullanarak eşleştirin

### Çocuk Hesabı
1. Uygulamayı açın ve "Kayıt Ol" seçin
2. Çocuk hesap türünü seçin
3. E-posta ve şifre ile kayıt olun
4. Ebeveyninizin verdiği eşleştirme kodunu girin
5. Usage Access iznini verin

### Uygulama Ayarları
1. Ebeveyn panelinde "Uygulama Ayarları"na gidin
2. Yüklü uygulamaları görüntüleyin
3. Her uygulama için günlük süre sınırı belirleyin

## İzinler

Uygulama aşağıdaki izinleri gerektirir:

- `PACKAGE_USAGE_STATS`: Uygulama kullanım verilerini okuma
- `SYSTEM_ALERT_WINDOW`: Sistem uyarı pencereleri gösterme
- `FOREGROUND_SERVICE`: Arka plan servisi çalıştırma
- `INTERNET`: Firebase bağlantısı

## Güvenlik

- Firebase Authentication ile güvenli kimlik doğrulama
- Firestore'da şifrelenmiş veri saklama
- Eşleştirme kodu ile güvenli bağlantı
- Usage Access izni ile sınırlı erişim
- **Gelişmiş Logout Sistemi:**
  - Merkezi LogoutManager ile tüm logout işlemleri
  - Session yönetimi - MANUEL çıkış sistemi (otomatik çıkış YOK)
  - Navigation stack temizleme
  - Hata yakalama ve kullanıcı bildirimleri
  - Extension fonksiyonlar ile basit kullanım
- **Offline Mode Desteği:**
  - İnternet bağlantısı olmasa bile uygulama çalışır
  - Local session cache sistemi
  - Otomatik online/offline geçiş
  - Network durumu bildirimleri

## Session Yönetimi

### 🔄 Offline-First Yaklaşım
- Local session cache ile internet olmadan çalışır
- Firebase verisi cache'lenir
- Network kesintilerinde kesintisiz deneyim
- Reconnect durumunda otomatik senkronizasyon

### 🛡️ Manuel Kontrol
- **OTOMATIK ÇIKIŞ YOK** - Kullanıcı kontrollü session
- Süre sınırı olmadan çalışır
- Manuel logout ile tam temizlik
- Session persistence ile uygulama kapatma/açmada devam eder

## Logout Sistemi Özellikleri

### 🛡️ Güvenlik
- Firebase Auth'dan tam çıkış
- Local session verilerinin temizlenmesi
- **MANUEL çıkış sistemi** - Otomatik çıkış YOK
- Güvenli navigation handling

### 📶 Offline Mode
- İnternet bağlantısı olmadan da çalışır
- Local session cache sistemi
- Network durumu takibi
- Otomatik online/offline geçiş

### 🔧 Kullanım Kolaylığı
- Extension fonksiyonlar (`performLogoutWithManager`, `performQuickLogout`)
- Merkezi LogoutManager sınıfı
- Onay dialog'u ile veya hızlı çıkış seçenekleri
- Otomatik hata yakalama ve bildirimi

### 📱 User Experience
- Loading göstergesi
- Başarı/hata toast mesajları
- Smooth navigation geçişleri
- Back stack temizleme
- Network durumu bildirimleri

## Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## İletişim

Proje Sahibi - [@your-username](https://github.com/your-username)

Proje Linki: [https://github.com/your-username/ZamanKumandasi](https://github.com/your-username/ZamanKumandasi)
