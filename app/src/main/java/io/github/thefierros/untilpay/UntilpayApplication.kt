package io.github.thefierros.untilpay

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] makes Hilt generate the
 * application-wide container that holds every app-scoped dependency.
 */
@HiltAndroidApp
class UntilpayApplication : Application()
