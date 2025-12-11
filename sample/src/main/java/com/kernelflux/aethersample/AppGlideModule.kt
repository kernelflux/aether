package com.kernelflux.aethersample

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

/**
 * Glide module configuration for sample app
 *
 * @author Aether Framework
 */
@GlideModule
class AppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Configure Glide options here if needed
        // For now, we use default options
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // Register custom components here if needed
        // For now, we use default components
    }

    override fun isManifestParsingEnabled(): Boolean {
        // Disable manifest parsing to avoid conflicts
        return false
    }
}
