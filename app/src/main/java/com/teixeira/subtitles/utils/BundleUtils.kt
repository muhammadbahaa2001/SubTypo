package com.teixeira.subtitles.utils

import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
  return BundleCompat.getParcelable(this, key, T::class.java)
}
