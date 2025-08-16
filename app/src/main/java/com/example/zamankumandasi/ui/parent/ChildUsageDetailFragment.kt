package com.example.zamankumandasi.ui.parent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.zamankumandasi.databinding.FragmentChildUsageDetailBinding
import com.example.zamankumandasi.ui.adapter.AppUsageAdapter
import com.example.zamankumandasi.ui.viewmodel.AppUsageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChildUsageDetailFragment : Fragment() {
    private var _binding: FragmentChildUsageDetailBinding? = null
    private val binding get() = _binding!!

    private val appUsageViewModel: AppUsageViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChildUsageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val childId = arguments?.getString("childId")
        val childEmail = arguments?.getString("childEmail")
        binding.tvChildEmail.text = childEmail ?: "Çocuk"

        appUsageAdapter = AppUsageAdapter(onAppClick = {})
        binding.rvChildAppUsage.apply {
            adapter = appUsageAdapter
            layoutManager = LinearLayoutManager(context)
        }

        if (childId != null) {
            appUsageViewModel.loadAppUsageByUser(childId)
        }

        appUsageViewModel.appUsageList.observe(viewLifecycleOwner) { usageList ->
            appUsageAdapter.submitList(usageList)
            binding.tvNoUsage.visibility = if (usageList.isEmpty()) View.VISIBLE else View.GONE
        }
        appUsageViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
