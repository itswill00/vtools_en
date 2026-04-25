package com.omarea.vtools.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.omarea.vtools.databinding.FragmentCpuModesContentBinding
import top.yukonga.miuix.kmp.theme.MiuixTheme

class FragmentCpuModes : Fragment() {
    private var _binding: FragmentCpuModesContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCpuModesContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tunerComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        binding.tunerComposeView.setContent {
            MiuixTheme {
                TunerPlaceholder()
            }
        }
    }

    @Composable
    fun TunerPlaceholder() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Tuner menu is ready for new content",
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MiuixTheme.textStyles.body2
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
