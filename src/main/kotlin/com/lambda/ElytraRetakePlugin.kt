package com.lambda

import com.lambda.client.plugin.api.Plugin
import com.lambda.modules.ElytraRetake

internal object ElytraRetakePlugin : Plugin() {

    override fun onLoad() {
        modules.add(ElytraRetake)
    }

    override fun onUnload() {
        
    }
}