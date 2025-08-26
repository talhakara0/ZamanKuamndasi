# Google Play Console In-App Purchase Kurulumu

## Test Ürün ID'leri

Bu uygulamada kullanılan product ID'ler Google Play Console'da tanımlanmalıdır:

### 1. Premium Aylık (`premium_monthly`)
- **Tür**: Managed Product (Tüketilemez)
- **Fiyat**: ₺29,99
- **Açıklama**: "Aylık premium üyelik - reklamları kaldırır ve tüm özelliklere erişim sağlar"

### 2. Premium Yıllık (`premium_yearly`) 
- **Tür**: Managed Product (Tüketilemez)
- **Fiyat**: ₺89,99 (Aylık plana göre %60 tasarruf)
- **Açıklama**: "Yıllık premium üyelik - reklamları kaldırır ve tüm özelliklere erişim sağlar"

### 3. Premium Yaşam Boyu (`premium_lifetime`)
- **Tür**: Managed Product (Tüketilemez) 
- **Fiyat**: ₺249,99
- **Açıklama**: "Yaşam boyu premium erişim - reklamları kaldırır ve tüm özelliklere erişim sağlar"

## Google Play Console Kurulum Adımları

1. **Google Play Console'a giriş yapın**
2. **Uygulama seçin** (veya yeni uygulama oluşturun)
3. **Kazanç > In-app products** sekmesine gidin
4. **Ürün oluştur** butonuna tıklayın
5. Her bir product ID için:
   - Product ID'yi girin (premium_monthly, premium_yearly, premium_lifetime)
   - Ürün adını ve açıklamasını girin
   - Fiyatı belirleyin
   - **Aktif** olarak ayarlayın

## Test Kullanıcıları

Test aşamasında satın alımları test etmek için:

1. **Kazanç > Licence testing** sekmesine gidin
2. Test Gmail hesaplarınızı ekleyin
3. **License response** ayarını "LICENSED" olarak ayarlayın

## Test Kartları

Google Play, test satın alımları için özel test kartları sağlar:
- **Kart numarası**: 4111 1111 1111 1111
- **Son kullanma**: Gelecekteki herhangi bir tarih
- **CVC**: 123

## Önemli Notlar

- **Test ortamında**: Gerçek ödeme yapılmaz
- **Production ortamında**: Gerçek ödemeler yapılır
- Bu product ID'ler `BillingManager.kt` dosyasında tanımlanmıştır
- Production'da çıkmadan önce "Draft" durumundaki ürünlerin "Active" yapılması gerekir

## Debug Test

Debug APK ile test yaparken:
- Test hesabı kullanın
- Google Play Console'da test kullanıcısı olarak kayıtlı olun
- Test cihazında Google Play Store'da giriş yapmış olun
