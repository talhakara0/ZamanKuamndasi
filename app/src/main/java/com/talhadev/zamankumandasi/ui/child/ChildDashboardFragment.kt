package com.talhadev.zamankumandasi.ui.child

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.delay
import com.talhadev.zamankumandasi.R
import com.talhadev.zamankumandasi.data.model.AppUsage
import com.talhadev.zamankumandasi.databinding.FragmentChildDashboardBinding
import com.talhadev.zamankumandasi.ads.AdManager
import com.talhadev.zamankumandasi.service.AppUsageService
import com.talhadev.zamankumandasi.ui.UsageAccessActivity
import com.talhadev.zamankumandasi.ui.adapter.AppUsageAdapter
import com.talhadev.zamankumandasi.ui.viewmodel.AppUsageViewModel
import com.talhadev.zamankumandasi.ui.viewmodel.AuthViewModel
import com.talhadev.zamankumandasi.utils.SimpleLogout
import com.talhadev.zamankumandasi.util.PermissionHelper
import com.talhadev.zamankumandasi.ui.PermissionCheckDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChildDashboardFragment : Fragment() {

    private var _binding: FragmentChildDashboardBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter
    
    // LogoutManager removed; using SimpleLogout utility

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("talha", "ChildDashboardFragment açıldı")

        // Classic menü yaklaşımı
        setHasOptionsMenu(true)
        
        // Eşleşme durumunu sıfırla (önceki ekranlardan kalan değerleri temizle)
        authViewModel.clearPairingState()

        // İZİN KONTROLÜ - TÜM İZİNLER VERİLMEDEN DEVAM ETME!
        checkAllRequiredPermissions()

        setupViews()
        setupRecyclerView()
        observeCurrentUser()
        observeAuthState()
        observeAppUsage()
    }

    /**
     * Tüm gerekli izinleri kontrol eder ve eksik izinler varsa izin isteyen sekmeye yönlendirir
     */
    private fun checkAllRequiredPermissions() {
        Log.i("ChildDashboard", "🔍 İZİN KONTROLÜ BAŞLADI - TÜM İZİNLER KONTROL EDİLİYOR...")
        
        val permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        
        Log.i("ChildDashboard", "📊 İZİN DURUMU:")
        Log.i("ChildDashboard", "📊 Usage Stats: ${permissionStatus.usageStats}")
        Log.i("ChildDashboard", "🖼️ Overlay: ${permissionStatus.overlay}")
        Log.i("ChildDashboard", "♿ Accessibility: ${permissionStatus.accessibility}")
        Log.i("ChildDashboard", "🔋 Battery: ${permissionStatus.batteryOptimization}")
        Log.i("ChildDashboard", "🎯 TÜM İZİNLER VERİLDİ Mİ? ${permissionStatus.allGranted}")
        
        if (!permissionStatus.allGranted) {
            Log.w("ChildDashboard", "⚠️ EKSİK İZİNLER TESPİT EDİLDİ - İZİN İSTEYEN SEKMEYE YÖNLENDİRİLİYOR!")
            
            // Eksik izinler varsa izin isteyen sekmeye yönlendir
            showPermissionDialog()
        } else {
            Log.i("ChildDashboard", "✅ TÜM İZİNLER VERİLDİ - NORMAL DEVAM EDİLİYOR")
            // Tüm izinler var, normal işlemlere devam et
            startAppUsageService()
        }
    }

    /**
     * İzin dialog'unu gösterir ve eksik izinleri ister
     */
    private fun showPermissionDialog() {
        Log.i("ChildDashboard", "🔘 İZİN DIALOG'U GÖSTERİLİYOR...")
        
        PermissionCheckDialog.newInstance {
            Log.i("ChildDashboard", "✅ İZİNLER VERİLDİ - SERVİS BAŞLATILIYOR")
            // İzinler verildikten sonra servis başlat
            startAppUsageService()
        }.show(parentFragmentManager, "ChildDashboardPermissionDialog")
    }

    private fun setupViews() {
        // Toolbar'ı Activity'ye set et
        (requireActivity() as androidx.appcompat.app.AppCompatActivity).setSupportActionBar(binding.toolbar)
        
        // Toolbar menü click listener'ı ekle
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    Log.d("talha", "ChildDashboard: Toolbar çıkış menu item seçildi")
                    SimpleLogout.confirmAndSignOut(this, authViewModel)
                    true
                }
                R.id.action_premium -> {
                    findNavController().navigate(R.id.action_childDashboardFragment_to_purchaseFragment)
                    true
                }
                else -> false
            }
        }
        
        // Refresh butonu
        binding.btnRefresh.setOnClickListener {
            // Yenile butonuna basılınca arada reklam göster (frekans limiti geçerliyse)
            if (authViewModel.currentUser.value?.isPremium != true) {
                AdManager.maybeShowInterstitial(requireActivity())
            }
            checkAndLoadUsageData()
        }

        // Eşleşme butonu - listener'ı observeCurrentUser'da ayarlayacağız
        // binding.btnPairNow.setOnClickListener artık burada yok
    }
    
    private fun checkNetworkStatus() {
        // Network kontrolü tamamen devre dışı - internet var kabul ediliyor
        Log.d("talha", "Network: ✅ İnternet kontrolü devre dışı (varsayılan: bağlı)")
    }

    private fun showPairingDialog() {
        // Kodu tamamen temizleyip yeniden yazıyoruz
        
        // Dialog view'ını inflate et
        val dialogView = layoutInflater.inflate(R.layout.dialog_pairing_code, null)
        val til = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilPairingCode)
        val et = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPairingCode)
        val progress = dialogView.findViewById<android.widget.ProgressBar>(R.id.progress)
        
        // Dialog objesini oluştur
        val alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Ebeveyn ile Eşleş")
            .setView(dialogView)
            .setCancelable(false) // Geri tuşuyla kapatılamaz
            .setPositiveButton("Eşleş", null) // Listener null, aşağıda override edilecek
            .setNegativeButton("İptal") { dialog, _ -> 
                dialog.dismiss() 
                authViewModel.clearPairingState()
            }
            .create()

        // Dialog gösterildiğinde yapılacaklar
        alertDialog.setOnShowListener { dialog ->
            val positiveButton = (dialog as android.app.AlertDialog).getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            
            // Pozitif buton için tıklama işleyicisi
            positiveButton.setOnClickListener { 
                // Önce hatayı temizle
                til.error = null
                
                // Kodu doğrula
                val code = et.text?.toString()?.trim() ?: ""
                when {
                    code.isEmpty() -> {
                        til.error = "Eşleştirme kodu gerekli"
                    }
                    code.length != 6 -> {
                        til.error = "Eşleştirme kodu 6 haneli olmalı"
                    }
                    !code.all { it.isDigit() } -> {
                        til.error = "Sadece rakam girmelisiniz"
                    }
                    else -> {
                        // Doğrulama başarılı, eşleştirme yap
                        progress.visibility = View.VISIBLE
                        positiveButton.isEnabled = false
                        authViewModel.pairWithParent(code)
                    }
                }
            }
        }
        
        // Dialog açılmadan önce pairing state'i temizle
        authViewModel.clearPairingState()
        
        // Dialog'u göster
        alertDialog.show()
        
        // State observer tanımla - sadece bu dialog için
        val stateObserver = object : androidx.lifecycle.Observer<AuthViewModel.PairingState?> {
            override fun onChanged(state: AuthViewModel.PairingState?) {
                Log.d("talha", "PairingState değişti: $state")
                when (state) {
                    is AuthViewModel.PairingState.Loading -> {
                        Log.d("talha", "Eşleştirme işlemi başladı")
                        progress.visibility = View.VISIBLE
                        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    }
                    is AuthViewModel.PairingState.Success -> {
                        Log.d("talha", "Eşleştirme başarılı: ${state.user}")
                        
                        // Eşleşme başarılı mesajı göster
                        Toast.makeText(requireContext(), "Ebeveyn ile başarıyla eşleştirildi!", Toast.LENGTH_LONG).show()
                        
                        // UI güncelle
                        binding.tvParentInfo.text = "Ebeveyn bilgisi yükleniyor..."
                        binding.btnPairNow.visibility = View.GONE
                        
                        // Parent bilgisini yükle
                        authViewModel.loadParentInfo(state.user.id)
                        
                        // Dialog'u kapat
                        alertDialog.dismiss()
                        
                        // State'i temizle
                        authViewModel.clearPairingState()
                    }
                    is AuthViewModel.PairingState.Error -> {
                        Log.e("talha", "Eşleştirme hatası: ${state.message}")
                        
                        // Eşleşme hatası
                        progress.visibility = View.GONE
                        til.error = state.message
                        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                    null -> {
                        Log.d("talha", "PairingState temizlendi (null)")
                        // Başlangıç durumu - hiçbir şey yapma
                        progress.visibility = View.GONE
                    }
                }
            }
        }
        
        // Observer'ı ekle
        authViewModel.pairingState.observe(viewLifecycleOwner, stateObserver)
        
        // Dialog kapandığında observer'ı kaldır ve state'i temizle
        alertDialog.setOnDismissListener {
            Log.d("talha", "Dialog kapatıldı - observer kaldırılıyor")
            authViewModel.pairingState.removeObserver(stateObserver)
            // State'i sadece başarısız durumlarda veya iptal edildiğinde temizle
            // Başarılı durumda zaten yukarıda temizleniyor
        }
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter(
            onAppClick = { appUsage ->
                // Uygulama kullanım detayına git
            }
        )

        binding.rvAppUsage.apply {
            adapter = appUsageAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeCurrentUser() {
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            Log.d("talha", "observeCurrentUser: user = $user")
            user?.let {
                // Reklam premium durumu
                AdManager.setPremium(it.isPremium)
                // Çocuk ismini göster, eğer yoksa email'i göster
                val displayName = if (it.name.isNotEmpty()) {
                    // Türkçe karakterleri koruyarak ismi göster
                    it.name
                } else {
                    "İsimsiz Çocuk"
                }
                binding.tvUserEmail.text = displayName
                
                // Ebeveyn eşleşme durumunu kontrol et
                if (it.parentId == null) {
                    // Henüz eşleştirilmemiş
                    binding.tvParentInfo.text = "Henüz eşleştirilmedi"
                    binding.btnPairNow.visibility = View.VISIBLE
                    
                    // Eşleşme butonuna tıklama işleyicisini yeniden ayarla
                    binding.btnPairNow.setOnClickListener {
                        Log.d("talha", "Eşleşme butonuna tıklandı - showPairingDialog çağrılıyor")
                        showPairingDialog()
                    }
                } else {
                    // Eşleştirilmiş - parent bilgisini yükle
                    binding.tvParentInfo.text = "Ebeveyn bilgisi yükleniyor..."
                    binding.btnPairNow.visibility = View.GONE
                    
                    // Parent bilgisini yükle
                    authViewModel.loadParentInfo(it.id)
                }
                
                appUsageViewModel.loadAppUsageByUser(it.id)
                
                // Otomatik olarak kullanım verilerini de topla (limitleri dahil)
                lifecycleScope.launch {
                    delay(500) // 500ms bekle (kullanıcı bilgileri yüklensin)
                    Log.d("talha", "Kullanıcı yüklendi, otomatik kullanım verisi toplanıyor")
                    checkAndLoadUsageData()
                }
            }
        }
        
        // Parent bilgisini observe et
        authViewModel.parentInfo.observe(viewLifecycleOwner) { parent ->
            val currentUser = authViewModel.currentUser.value
            if (parent != null) {
                binding.tvParentInfo.text = "Ebeveyn: ${parent.email}"
            } else if (currentUser?.parentId != null) {
                // Parent ID'si var ama parent bilgisi bulunamadı
                binding.tvParentInfo.text = "Ebeveyn bilgisi bulunamadı"
            }
        }
    }

    /**
     * QueryEvents tabanlı kullanım ölçümü - İyileştirilmiş versiyon
     */
    private fun checkAndLoadUsageData() {
        Log.d("talha", "Yenile butonuna basıldı, usage verisi toplanıyor")
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000 // Son 24 saat

        // Önce UsageStats ile günlük toplam kullanım verilerini al (daha güvenilir)
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val usageMap = mutableMapOf<String, Long>()
        
        // UsageStats'tan toplam kullanım sürelerini al
        usageStats.forEach { stats ->
            if (stats.totalTimeInForeground > 0) {
                usageMap[stats.packageName] = stats.totalTimeInForeground // Bu zaten milisaniye cinsinden
                Log.d("talha", "${stats.packageName}: ${stats.totalTimeInForeground}ms (${stats.totalTimeInForeground/1000/60}dk)")
            }
        }

        // Ek olarak Events ile eksik verileri tamamla
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        var lastEventTimeMap = mutableMapOf<String, Long>()

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastEventTimeMap[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val start = lastEventTimeMap[event.packageName]
                    if (start != null) {
                        val duration = event.timeStamp - start // Milisaniye cinsinden
                        // Sadece UsageStats'ta olmayan uygulamalar için event verisi kullan
                        if (!usageMap.containsKey(event.packageName)) {
                            usageMap[event.packageName] = usageMap.getOrDefault(event.packageName, 0) + duration
                        }
                        lastEventTimeMap.remove(event.packageName)
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    // Ekran kapanınca açık kalan uygulamaları da hesapla
                    lastEventTimeMap.forEach { (pkg, startTime) ->
                        val duration = event.timeStamp - startTime
                        if (duration > 0 && !usageMap.containsKey(pkg)) {
                            usageMap[pkg] = usageMap.getOrDefault(pkg, 0) + duration
                        }
                    }
                    lastEventTimeMap.clear()
                }
            }
        }

        // Hala açık olan uygulamaları hesapla
        lastEventTimeMap.forEach { (pkg, startTime) ->
            val duration = endTime - startTime
            if (duration > 0 && !usageMap.containsKey(pkg)) {
                usageMap[pkg] = usageMap.getOrDefault(pkg, 0) + duration
            }
        }

        Log.d("talha", "Toplam uygulama sayısı: ${usageMap.size}")
        usageMap.forEach { (pkg, time) ->
            Log.d("talha", "$pkg: ${time}ms (${time/1000/60}dk)")
        }

        // Minimum kullanım süresi filtresi (30 saniyeden az olanları hariç tut)
        val filteredUsageMap = usageMap.filter { it.value >= 30000 } // 30 saniye = 30000ms

        val usageList = filteredUsageMap.map { (pkg, dur) ->
            AppUsage(
                appName = getAppNameFromPackage(requireContext(), pkg),
                packageName = pkg,
                usedTime = dur // Artık milisaniye cinsinden
            )
        }

        // 🔹 RTDB'ye yaz (her uygulama için) - Akıllı veri birleştirme ile
        authViewModel.currentUser.value?.let { user ->
            // Firebase'den mevcut kullanım verilerini çek (limitleri dahil)
            lifecycleScope.launch {
                try {
                    val existingUsageList = appUsageViewModel.getAppUsageByUser(user.id)
                    Log.d("talha", "Firebase'den ${existingUsageList.size} uygulama verisi çekildi")
                    
                    // Kullanım verilerini Firebase verileriyle birleştir
                    val mergedUsageList = usageList.map { appUsage ->
                        val existingUsage = existingUsageList.find { it.packageName == appUsage.packageName }
                        if (existingUsage != null) {
                            // Mevcut veri var - limitleri koru ve kullanım süresini güncelle
                            val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
                            val lastUsedDay = existingUsage.lastUsed / (24 * 60 * 60 * 1000)
                            
                            if (today == lastUsedDay) {
                                // Aynı gün - maksimum kullanım süresini kullan
                                existingUsage.copy(
                                    usedTime = maxOf(existingUsage.usedTime, appUsage.usedTime),
                                    lastUsed = System.currentTimeMillis(),
                                    isBlocked = maxOf(existingUsage.usedTime, appUsage.usedTime) >= existingUsage.dailyLimit && existingUsage.dailyLimit > 0
                                )
                            } else {
                                // Yeni gün - kullanım süresini sıfırla
                                existingUsage.copy(
                                    usedTime = appUsage.usedTime,
                                    lastUsed = System.currentTimeMillis(),
                                    isBlocked = appUsage.usedTime >= existingUsage.dailyLimit && existingUsage.dailyLimit > 0
                                )
                            }
                        } else {
                            // Yeni uygulama - sadece kullanım verisi
                            appUsage.copy(
                                userId = user.id,
                                id = "${user.id}_${appUsage.packageName.replace(".", "_")}",
                                lastUsed = System.currentTimeMillis()
                            )
                        }
                    }
                    
                    // Firebase'e güncellenmiş verileri yaz
                    mergedUsageList.forEach { finalAppUsage ->
                        try {
                            appUsageViewModel.saveAppUsage(finalAppUsage)
                            Log.d("talha", "Firebase'e yazma başarılı: ${finalAppUsage.id} - ${finalAppUsage.usedTime}ms, Limit: ${finalAppUsage.dailyLimit}ms")
                        } catch (e: Exception) {
                            Log.e("talha", "Firebase'e yazma hatası: ${e.message}")
                        }
                    }
                    
                    // UI'ya güncellenmiş verileri gönder (limitleri dahil)
                    val sortedUsageList = mergedUsageList.sortedByDescending { it.usedTime }
                    activity?.runOnUiThread {
                        appUsageAdapter.submitList(sortedUsageList)
                        Log.d("talha", "UI'ya ${sortedUsageList.size} uygulama yüklendi (limitler dahil)")
                    }
                    
                } catch (e: Exception) {
                    Log.e("talha", "Firebase'den veri çekme hatası: ${e.message}")
                    // Hata durumunda sadece cihaz verilerini göster
                    val sortedUsageList = usageList.sortedByDescending { it.usedTime }
                    activity?.runOnUiThread {
                        appUsageAdapter.submitList(sortedUsageList)
                    }
                }
            }
        } ?: Log.e("talha", "Kullanıcı null, Firebase'e yazılamadı!")
    }

    // 🔹 Paket adından uygulama adını alma fonksiyonu
    private fun getAppNameFromPackage(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun observeAppUsage() {
        appUsageViewModel.appUsageList.observe(viewLifecycleOwner) { usageList ->
            if (usageList.isNotEmpty()) {
                binding.tvNoUsage.visibility = View.GONE
                appUsageAdapter.submitList(usageList)
                Log.d("talha", "UI'ya ${usageList.size} uygulama yüklendi")
            } else {
                binding.tvNoUsage.visibility = View.VISIBLE
                Log.d("talha", "UI'da kullanım verisi yok")
            }
        }

        appUsageViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    // Eski deprecated menü fonksiyonları kaldırıldı - modern MenuProvider kullanılıyor
    
    private fun performDirectLogout() {
        Log.d("talha", "ChildDashboard: performDirectLogout başlatıldı")

        // Onay dialogu göster ve onaylanırsa signOut çağır
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Çıkış Yap")
            .setMessage("Çıkış yapmak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { dialog, _ ->
                Log.d("talha", "ChildDashboard: Çıkış onaylandı - ViewModel.signOut çağrılıyor")
                dialog.dismiss()
                authViewModel.signOut()
            }
            .setNegativeButton("Hayır") { dialog, _ ->
                Log.d("talha", "ChildDashboard: Çıkış iptal edildi")
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    Log.d("talha", "AuthState: Loading")
                }
                is AuthViewModel.AuthState.SignedOut -> {
                    Log.d("talha", "AuthState: SignedOut - navigate to login with cleared backstack")
                    try {
                        val navOptions = androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .setLaunchSingleTop(true)
                            .build()

                        findNavController().navigate(R.id.loginFragment, null, navOptions)
                        Toast.makeText(context, "Başarıyla çıkış yapıldı", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("talha", "observeAuthState nav error: ${e.message}")
                    }
                }
                is AuthViewModel.AuthState.Error -> {
                    Log.e("talha", "AuthState Error: ${state.message}")
                    Toast.makeText(context, "Çıkış hatası: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is AuthViewModel.AuthState.Success -> {
                    // ignore
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("ChildDashboard", "🔄 onResume - İZİN KONTROLÜ YAPILIYOR...")
        
        // Her onResume'de izin kontrolü yap - eksik izinler varsa dialog göster
        val permissionStatus = PermissionHelper.checkAllRequiredPermissions(requireContext())
        
        if (!permissionStatus.allGranted) {
            Log.w("ChildDashboard", "⚠️ onResume: EKSİK İZİNLER TESPİT EDİLDİ!")
            showPermissionDialog()
        } else {
            Log.i("ChildDashboard", "✅ onResume: TÜM İZİNLER VERİLDİ")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Classic menü fonksiyonları
    override fun onCreateOptionsMenu(menu: android.view.Menu, inflater: android.view.MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        Log.d("talha", "ChildDashboard: onCreateOptionsMenu çağrıldı")
        inflater.inflate(R.menu.menu_child_dashboard, menu)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                Log.d("talha", "ChildDashboard: Options menüden çıkış seçildi")
                SimpleLogout.confirmAndSignOut(this, authViewModel)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startAppUsageService() {
        val usageStatsManager =
            requireContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (24 * 60 * 60 * 1000)

        try {
            val queryUsageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )

            Log.d("talha", "Usage Access kontrolü: ${queryUsageStats.size} uygulama bulundu")

            if (queryUsageStats.isEmpty()) {
                val intent = Intent(requireContext(), UsageAccessActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(requireContext(), AppUsageService::class.java).apply {
                    action = "START_TRACKING"
                }
                requireContext().startService(intent)

                authViewModel.currentUser.value?.let { user ->
                    checkAndLoadUsageData()
                }
            }
        } catch (e: Exception) {
            Log.d("talha", "Usage Access hatası: ${e.message}")
            e.printStackTrace()
        }
    }

    // İzin kontrolü kaldırıldı - sadece MainActivity'de yapılıyor


}
