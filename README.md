# Zaman Kumandası - Android Uygulaması

Ebeveynlerin çocuklarının telefon kullanımını kontrol etmelerini sağlayan Android uygulaması.

## Özellikler

### 🔐 Kimlik Doğrulama
- Firebase Authentication ile e-posta/şifre kayıt ve giriş
- Ebeveyn ve çocuk hesap türleri
- Eşleştirme kodu ile ebeveyn-çocuk bağlantısı

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
