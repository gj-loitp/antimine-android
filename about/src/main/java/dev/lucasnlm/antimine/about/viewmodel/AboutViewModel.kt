package dev.lucasnlm.antimine.about.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.lucasnlm.antimine.about.R
import dev.lucasnlm.antimine.core.viewmodel.StatelessViewModel
import dev.lucasnlm.antimine.licenses.LicenseActivity
import dev.lucasnlm.antimine.tutorial.TutorialActivity

class AboutViewModel(
    private val application: Application,
) : StatelessViewModel<AboutEvent>() {

    override fun onEvent(event: AboutEvent) {
        when (event) {
            AboutEvent.ThirdPartyLicenses -> {
                openLicensesActivity()
            }
            AboutEvent.SourceCode -> {
                openSourceCode()
            }
            AboutEvent.Translators -> {
                openCrowdin()
            }
            AboutEvent.Tutorial -> {
                openTutorial()
            }
        }
    }

    private fun openLicensesActivity() {
        val context = application.applicationContext
        val intent = Intent(context, LicenseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun openTutorial() {
        val context = application.applicationContext
        val intent = Intent(context, TutorialActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    private fun openSourceCode() {
        val context = application.applicationContext
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context.applicationContext, R.string.unknown_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCrowdin() {
        val context = application.applicationContext
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(CROWDIN_URL)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context.applicationContext, R.string.unknown_error, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val SOURCE_CODE = "https://github.com/lucasnlm/antimine-android"
        private const val CROWDIN_URL = "https://crowdin.com/project/antimine-android"
    }
}
