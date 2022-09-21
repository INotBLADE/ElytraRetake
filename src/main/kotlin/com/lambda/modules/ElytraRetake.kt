package com.lambda.modules

import com.lambda.ElytraRetakePlugin
import com.lambda.client.module.Category
import com.lambda.client.plugin.api.PluginModule
import com.lambda.client.module.modules.movement.ElytraReplace
import com.lambda.client.util.InfoCalculator
import com.lambda.client.util.TpsCalculator
import com.lambda.client.util.text.MessageSendHelper
import com.lambda.client.util.threads.safeListener
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraft.inventory.ClickType
import kotlin.math.roundToInt

internal object ElytraRetake : PluginModule(
    name = "ElytraRetake",
    category = Category.MOVEMENT,
    description = "Retake your elytras for fix rubberbanding on some servers",
    pluginMain = ElytraRetakePlugin
){
    private val page by setting("Page", Page.Main)
    private enum class Page {
        Main, Messages, Other
    }

    private val mode by setting("Mode", Mode.Simple, { page == Page.Main })
    private enum class Mode {
        Simple, Safe
    }

    //Main
    private val Timeout = setting( "Timeout (tick)", 5, 5..40, 1, { page == Page.Main })
    private val HighPing = setting("HighPingFix", false, { page == Page.Main })
    private val LowTps = setting("LowTpsFix", false, { page == Page.Main })


    //Messages
    private val PlaySound = setting("Play sound", true, { page == Page.Messages })
    private val SoundPitch = setting("Sound pitch", 1f, 0.5f..1.5f, 0.1f, { PlaySound.value && page == Page.Messages })
    private val ShowMessages = setting("Show messages", true, { page == Page.Messages })
    private val ElytraReplaceWarning = setting("ElytraReplace warning", true, { ShowMessages.value && page == Page.Messages })
    private val DontFlyWarning = setting("Don't fly warning", true, { ShowMessages.value && page == Page.Messages })


    //Other
    private val ElytraReplaceAutoDisable = setting("ElytraReplace autodisable", false, { page == Page.Other })


    private var editTime = false
    private var time = 10

    init {
        safeListener<TickEvent.ClientTickEvent> {
            if(editTime)
            {
                time -= 1
            }

            if (time < 0)
            {
                editTime = false
                if (PlaySound.value)
                {
                    mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundPitch.value, 1.0f))
                }
                if(mode == Mode.Simple){
                    postSimple()
                }
                if(mode == Mode.Safe){
                    postSafe()
                }

            }
        }
    }

    init {
        onEnable {
            //Warning message
            if (ElytraReplace.isEnabled && ElytraReplaceAutoDisable.value)
            {
                ElytraReplace.disable()

                if(ElytraReplaceWarning.value)
                {
                    messageC("[ElytraRetake] : Elytra replace disabled")
                }

            }
            if (ElytraReplace.isEnabled)
            {
                if(ElytraReplaceWarning.value)
                {
                    messageC("[ElytraRetake] : ElytraReplace module not compatible with ElytraRetake")
                }
                ElytraRetake.disable()
            }
            if (!mc.player.isElytraFlying)
            {
                if (DontFlyWarning.value)
                {
                    messageC("[ElytraRetake-Warning] : You don't fly.")
                }
                ElytraRetake.disable()

            }else if (!ElytraReplace.isEnabled){

                var pingFix = (ClampInt(InfoCalculator.ping(), 0, 1000) / 100) * ToInt(HighPing.value)
                var tpsFix = ((20 - TpsCalculator.tickRate).roundToInt()) * ToInt(LowTps.value)

                time = Timeout.value + pingFix  + tpsFix
                editTime = true

                if(mode == Mode.Simple){
                    doSimple()
                }else if(mode == Mode.Safe){
                    doSafe()
                }

            }

        }
    }
    private fun doSimple() {
        mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, mc.player)
    }
    private fun doSafe() {
        mc.playerController.windowClick(0, 6, 0, ClickType.QUICK_MOVE, mc.player)
    }

    private fun postSimple() {
        mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, mc.player)
        ElytraRetake.disable()
    }
    private fun postSafe() {
        mc.playerController.windowClick(0, getElytraSlot(), 0, ClickType.QUICK_MOVE, mc.player)
        ElytraRetake.disable()
    }

    private fun getElytraSlot():Int
    {
        var slot = 0
        for (i in 9..44)
        {
            val stack = mc.player.inventory.getStackInSlot(i)
            if (stack.item == Items.ELYTRA)
            {
                slot = i
            }
        }
        return slot
    }


    private fun messageC(msg: String) {
        if (ShowMessages.value)
        {
            MessageSendHelper.sendChatMessage(msg)
        }
    }

    private fun ClampInt(value: Int, min: Int, max: Int): Int
    {
        if (value >= max) {
            return max
        }else if (value <= min){
            return min
        }else{
            return value
        }
    }
    private fun ToInt(value: Boolean): Int
    {
        if(value){
            return 1
        }else{
            return 0
        }
    }
}

