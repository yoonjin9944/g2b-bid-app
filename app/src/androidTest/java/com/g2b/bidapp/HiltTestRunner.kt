package com.g2b.bidapp

import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context
    ): android.app.Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)


}