/*
 * This file is part of SubTypo.
 *
 * SubTypo is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * SubTypo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SubTypo.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.teixeira.subtitles.activities.project

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.teixeira.subtitles.R
import com.teixeira.subtitles.activities.BaseActivity
import com.teixeira.subtitles.adapters.SubtitleListAdapter
import com.teixeira.subtitles.databinding.ActivityProjectBinding
import com.teixeira.subtitles.utils.ToastUtils
import com.teixeira.subtitles.utils.cancelIfActive
import com.teixeira.subtitles.viewmodels.SubtitlesViewModel
import com.teixeira.subtitles.viewmodels.VideoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base class for ProjectActivity that handles most activity related things.
 *
 * @author Felipe Teixeira
 */
abstract class BaseProjectActivity : BaseActivity() {

  private var _binding: ActivityProjectBinding? = null
  private var _isDestroying = false

  protected val coroutineScope = CoroutineScope(Dispatchers.Main)
  protected val videoViewModel by viewModels<VideoViewModel>()
  protected val subtitlesViewModel by viewModels<SubtitlesViewModel>()

  private val subtitleFilePicker =
    registerForActivityResult(ActivityResultContracts.GetContent(), this::onPickSubtitleFile)

  private val subtitleFileExporter =
    registerForActivityResult(
      ActivityResultContracts.CreateDocument("text/*"),
      this::onExportSubtitleFile
    )

  protected val binding: ActivityProjectBinding
    get() = checkNotNull(_binding) { "Activity has been destroyed!" }

  protected val isDestroying: Boolean
    get() = _isDestroying

  override fun bindView(): View {
    _binding = ActivityProjectBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setSupportActionBar(binding.toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    configureListeners()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_project_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.itemId

    when (id) {
      R.id.menu_export -> {
        if (
          subtitlesViewModel.subtitles?.isEmpty() ?: true ||
            subtitlesViewModel.timedTextObjects.isEmpty()
        ) {
          ToastUtils.showShort(R.string.error_no_subtitles_to_export)
          return false
        }
        videoViewModel.pauseVideo()
        val timedTextObject = subtitlesViewModel.selectedTimedTextObject
        if (timedTextObject != null) {
          subtitleFileExporter.launch(
            timedTextObject.timedTextInfo.name + timedTextObject.timedTextInfo.format.extension
          )
        }
      }
      R.id.menu_import -> subtitleFilePicker.launch("*/*")
      else -> {}
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onDestroy() {
    _isDestroying = true
    preDestroy()
    super.onDestroy()
    postDestroy()
  }

  protected open fun preDestroy() {}

  protected open fun postDestroy() {
    coroutineScope.cancelIfActive("Activity has been destroyed")
    _binding = null
  }

  protected open fun configureListeners() {}

  protected open fun onPickSubtitleFile(uri: Uri?) {}

  protected open fun onExportSubtitleFile(uri: Uri?) {}

  protected abstract fun requireSubtitleListAdapter(): SubtitleListAdapter

  companion object {
    const val KEY_PROJECT = "project"
  }
}
