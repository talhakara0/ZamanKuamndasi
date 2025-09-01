# Yeni Uygulama Engelleme Sistemi Kurulum Rehberi

## Genel Bakış
Bu sistem, çocuk hesaplarında limit dolmuş uygulamaları **overlay teknolojisi** kullanarak engellemek için tasarlanmıştır. Erişilebilirlik servisi yerine daha basit ve güvenilir bir yaklaşım kullanır.

## Kurulum Adımları

### 1. Gerekli İzinler
- `PACKAGE_USAGE_STATS` - Uygulama kullanım istatistiklerini okumak için
- `SYSTEM_ALERT_WINDOW` - Overlay göstermek için (Diğer uygulamalar üzerinde görüntüle)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Battery optimizasyonunu kapatmak için

### 2. Otomatik İzin Kontrolü
1. Uygulamayı açın
2. Çocuk hesabı ile giriş yapın
3. **"İzin Kontrolü" dialog'u otomatik olarak açılacak**
4. Eksik olan izinler için "Ayarla" butonlarına tıklayın
5. Her izin için ilgili ayar sayfasına yönlendirileceksiniz
6. İzinleri verdikten sonra "Yenile" butonuna tıklayın
7. Tüm izinler verildiğinde "Devam Et" butonu görünecek

### 3. Manuel İzin Verme (Gerekirse)
**Overlay İzni (Diğer uygulamalar üzerinde görüntüle):**
- Ayarlar > Uygulamalar > ZamanKumandasi > Diğer uygulamalar üzerinde görüntüle > Etkinleştir
- **Bu izin sayesinde limit dolduğunda uyarı gösterebiliriz**

**Kullanım İstatistikleri:**
- Ayarlar > Dijital Refah ve Ebeveyn Kontrolü > Kullanım İstatistikleri > ZamanKumandasi

**Battery Optimizasyonu:**
- Ayarlar > Uygulamalar > ZamanKumandasi > Battery > Battery optimizasyonunu kapat

## Sistem Bileşenleri

### 1. AppUsageService
- Her 5 saniyede bir uygulama kullanımını takip eder
- Limit aşıldığında hemen ana ekrana döner
- BlockerActivity'yi başlatır

### 2. OverlayBlockerService (YENİ!)
- Her 1 saniyede bir aktif uygulamayı kontrol eder
- Limit aşıldığında HEMEN ana ekrana döner
- Overlay gösterir ve tekrar ana ekrana yönlendirir
- **Erişilebilirlik servisi gerektirmez!**

### 3. AntiBypassService (SÜPER AGRESİF!)
- Her 0.2 saniyede bir kontrol eder
- Israrlı kullanıcılar için ek önlemler
- Aynı uygulamayı açmaya çalışırsa daha agresif olur
- **10-20 kata kadar ana ekrana döndürür!**

### 4. NuclearBombService (NÜKLEER SEVİYE!)
- Her 0.1 saniyede bir kontrol eder
- **50 kez ana ekrana döndürür!**
- 5 farklı nükleer saldırı fazı
- **Hiçbir şey kaçamaz!**

### 5. AppLimitCheckWorker
- Arka planda 15 dakikada bir kontrol yapar
- Ek güvenlik katmanı sağlar

### 6. NÜKLEER SEVİYE KORUMA KATMANLARI
- **AppUsageService**: 2 saniyede bir + 3 kez ana ekrana dön
- **OverlayBlockerService**: 1 saniyede bir + overlay uyarı
- **AntiBypassService**: 0.2 saniyede bir + 10-20 kez ana ekrana dön
- **NuclearBombService**: 0.1 saniyede bir + 50 kez ana ekrana dön
- **WorkManager**: 15 dakikada bir arka plan koruması

## Test Senaryoları

### Senaryo 1: Basit Limit Testi
1. Çocuk hesabı ile giriş yapın
2. Bir uygulamaya 1 dakika limit koyun
3. O uygulamayı açın ve 1 dakika kullanın
4. Limit aşıldığında uygulama kapanmalı ve ana ekrana dönmelisiniz

### Senaryo 2: Agresif Engelleme Testi
1. Overlay iznini verin
2. Limit dolmuş bir uygulamayı açmaya çalışın
3. **1 saniye içinde** ana ekrana dönmelisiniz
4. Tekrar açmaya çalışın - daha hızlı engellenecek

### Senaryo 3: NÜKLEER BOMB Testi
1. Limit dolmuş bir uygulamayı açmaya çalışın
2. **0.1 saniye içinde** 50 kez ana ekrana döndürülecek
3. 5 farklı nükleer saldırı fazı çalışacak
4. **Hiçbir şey kaçamayacak!**

### Senaryo 4: Israrlı Kullanıcı Testi
1. Aynı engellenmiş uygulamayı 3-4 kez açmaya çalışın
2. Sistem giderek daha agresif hale gelecek
3. 10-20 kata kadar ana ekrana döndürülecek

### Senaryo 5: WorkManager Testi
1. Uygulamayı arka plana alın
2. Limit dolmuş bir uygulamayı açmaya çalışın
3. WorkManager 15 dakika içinde kontrol edip engellemeli

## Sorun Giderme

### Otomatik İzin Kontrolü Çalışmıyor
- Çocuk hesabı ile giriş yaptığınızdan emin olun
- Uygulamayı yeniden başlatın
- Manuel olarak izinleri kontrol edin

### Erişilebilirlik Servisi Çalışmıyor
- Ayarlar > Erişilebilirlik > ZamanKumandasi Uygulama Engelleme servisinin etkin olduğundan emin olun
- Servisi kapatıp tekrar açın
- "İzin Kontrolü" dialog'unda "Yenile" butonuna tıklayın
- **Özel Durumlar:**
  - **Samsung cihazlar**: Ek güvenlik ayarları gerekebilir
  - **Huawei cihazlar**: Battery optimizasyonu ve otomatik başlatma ayarları
  - **Xiaomi cihazlar**: MIUI güvenlik ayarları ve otomatik başlatma
  - **Android 10+**: Background activity restrictions kapatılmalı

### Uygulamalar Engellenmiyor
- Kullanım istatistikleri izninin verildiğinden emin olun
- Battery optimizasyonunun kapatıldığından emin olun
- Uygulamayı yeniden başlatın
- Erişilebilirlik servisini yeniden etkinleştirin

### Limit Kontrolü Çalışmıyor
- AppUsageService'in çalıştığından emin olun
- Bildirim çubuğunda "Uygulama kullanımı takip ediliyor" mesajını görün
- Tüm izinlerin verildiğinden emin olun
- Gerekirse uygulamayı yeniden başlatın

### İzin Kontrolü Dialog'u Görünmüyor
- Çocuk hesabı ile giriş yapın
- Fragment'te onResume'da otomatik kontrol yapılır
- Manuel olarak "İzin Kontrolü" menüsünden açabilirsiniz

## Güvenlik Notları

1. **Erişilebilirlik Servisi**: Bu servis tüm ekran aktivitelerini görebilir, sadece güvenilir uygulamalarda kullanın
2. **Sistem İzinleri**: Bu izinler sistem seviyesinde çalışır, dikkatli kullanın
3. **Çocuk Güvenliği**: Sistem sadece çocuk hesaplarında çalışır, ebeveyn hesaplarında etkin değildir

## Performans Optimizasyonları

- Servis 5 saniyede bir kontrol yapar (battery optimizasyonu için)
- WorkManager 15 dakikada bir arka plan kontrolü yapar
- Spam önleme için aynı uygulama 5 saniye içinde tekrar engellenmez

## Destek

Herhangi bir sorun yaşarsanız:
1. Logları kontrol edin (Android Studio Logcat)
2. Uygulamayı yeniden başlatın
3. İzinleri yeniden verin
4. Erişilebilirlik servisini yeniden etkinleştirin
