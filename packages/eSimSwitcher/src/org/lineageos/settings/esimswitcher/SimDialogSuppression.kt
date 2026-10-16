/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.esimswitcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Holds Settings' automatic SIM dialogs until the complete mode change has finished. */
internal class SimDialogSuppression(private val context: Context) : Closeable {
    private val connected = CountDownLatch(1)
    @Volatile private var available = false
    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                available = true
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) {}

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }

            override fun onBindingDied(name: ComponentName) {
                connected.countDown()
            }
        }
    private var bound = false

    fun acquire() {
        try {
            bound =
                context.bindService(
                    Intent()
                        .setClassName(
                            "com.android.settings",
                            "com.android.settings.sim.SimDialogSuppressionService",
                        ),
                    connection,
                    Context.BIND_AUTO_CREATE,
                )
            if (
                bound &&
                    (!connected.await(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS) || !available)
            ) {
                Log.w(TAG, "Settings SIM dialog connection unavailable")
                close()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "Settings SIM dialog coordination unavailable", e)
        }
    }

    override fun close() {
        if (bound) {
            bound = false
            context.unbindService(connection)
        }
    }

    private companion object {
        const val TAG = "SimDialogSuppression"
        const val CONNECTION_TIMEOUT_SECONDS = 5L
    }
}
