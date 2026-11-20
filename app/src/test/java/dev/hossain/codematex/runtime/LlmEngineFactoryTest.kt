@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.hossain.codematex.runtime

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.Display
import com.google.ai.edge.litertlm.EngineConfig
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class LlmEngineFactoryTest {
    private val config = ModelConfig(maxTokens = 128)
    private val systemInstruction = "You are a helpful assistant"

    @Test
    fun `createSession closes partial engine when conversation creation fails with non-JNI error`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeEngine = FakeInferenceEngine()
            fakeEngine.conversationToThrow = RuntimeException("Conversation creation failed")

            val nativeEngineFactory =
                object : NativeEngineFactory {
                    override fun create(config: EngineConfig): InferenceEngine = fakeEngine
                }
            val factory =
                DefaultLlmEngineFactory(
                    context = FakeContext(),
                    backendFallbackStrategy = DefaultBackendFallbackStrategy(),
                    nativeEngineFactory = nativeEngineFactory,
                )

            try {
                factory.createSession(
                    modelPath = "/data/model.bin",
                    preferredBackend = LlmEngine.Backend.GPU,
                    systemInstruction = systemInstruction,
                    config = config,
                )
            } catch (e: RuntimeException) {
                // Expected.
            }

            assertThat(fakeEngine.closed).isTrue()
        }

    @Test
    fun `createSession does not close engine on successful session creation`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeEngine = FakeInferenceEngine()
            val fakeConversation = FakeInferenceConversation()
            fakeEngine.createdConversations.add(fakeConversation)

            val nativeEngineFactory =
                object : NativeEngineFactory {
                    override fun create(config: EngineConfig): InferenceEngine = fakeEngine
                }
            val factory =
                DefaultLlmEngineFactory(
                    context = FakeContext(),
                    backendFallbackStrategy = DefaultBackendFallbackStrategy(),
                    nativeEngineFactory = nativeEngineFactory,
                )

            val session =
                factory.createSession(
                    modelPath = "/data/model.bin",
                    preferredBackend = LlmEngine.Backend.GPU,
                    systemInstruction = systemInstruction,
                    config = config,
                )

            // Success path should not close anything.
            assertThat(fakeEngine.closed).isFalse()
            assertThat(fakeConversation.closed).isFalse()
            assertThat(session.backend).isEqualTo(LlmEngine.Backend.GPU)
        }

    @Test
    fun `createFallbackSession advances NPU failure to GPU and records unsupported backend`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeEngine = FakeInferenceEngine()
            fakeEngine.createdConversations.add(FakeInferenceConversation())
            val fallbackStrategy = DefaultBackendFallbackStrategy()
            val factory =
                DefaultLlmEngineFactory(
                    context = FakeContext(),
                    backendFallbackStrategy = fallbackStrategy,
                    nativeEngineFactory =
                        object : NativeEngineFactory {
                            override fun create(config: EngineConfig): InferenceEngine = fakeEngine
                        },
                )

            val session =
                factory.createFallbackSession(
                    modelPath = "/data/model.bin",
                    failedBackend = LlmEngine.Backend.NPU,
                    systemInstruction = systemInstruction,
                    config = config,
                )

            assertThat(session.backend).isEqualTo(LlmEngine.Backend.GPU)
            assertThat(fallbackStrategy.isUnsupported(LlmEngine.Backend.NPU)).isTrue()
        }

    @Test
    fun `createSession closes native engine when cancelled during engine initialization`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeEngine = FakeInferenceEngine()
            lateinit var sessionJob: Job
            fakeEngine.onInitialize = {
                // Simulate caller coroutine cancellation while native initialize() is executing
                sessionJob.cancel()
            }

            val factory =
                DefaultLlmEngineFactory(
                    context = FakeContext(),
                    backendFallbackStrategy = DefaultBackendFallbackStrategy(),
                    nativeEngineFactory =
                        object : NativeEngineFactory {
                            override fun create(config: EngineConfig): InferenceEngine = fakeEngine
                        },
                )

            sessionJob =
                launch(start = CoroutineStart.LAZY) {
                    factory.createSession(
                        modelPath = "/data/model.bin",
                        preferredBackend = LlmEngine.Backend.GPU,
                        systemInstruction = systemInstruction,
                        config = config,
                    )
                }

            sessionJob.start()
            sessionJob.join()

            assertThat(sessionJob.isCancelled).isTrue()
            assertThat(fakeEngine.initialized).isTrue()
            assertThat(fakeEngine.closed).isTrue()
        }

    @Test
    fun `createSession closes conversation and engine when cancelled during conversation creation`() =
        runTest(UnconfinedTestDispatcher()) {
            val fakeEngine = FakeInferenceEngine()
            lateinit var sessionJob: Job
            fakeEngine.onCreateConversation = {
                // Simulate caller coroutine cancellation while conversation creation is executing
                sessionJob.cancel()
            }

            val factory =
                DefaultLlmEngineFactory(
                    context = FakeContext(),
                    backendFallbackStrategy = DefaultBackendFallbackStrategy(),
                    nativeEngineFactory =
                        object : NativeEngineFactory {
                            override fun create(config: EngineConfig): InferenceEngine = fakeEngine
                        },
                )

            sessionJob =
                launch(start = CoroutineStart.LAZY) {
                    factory.createSession(
                        modelPath = "/data/model.bin",
                        preferredBackend = LlmEngine.Backend.GPU,
                        systemInstruction = systemInstruction,
                        config = config,
                    )
                }

            sessionJob.start()
            sessionJob.join()

            assertThat(sessionJob.isCancelled).isTrue()
            assertThat(fakeEngine.closed).isTrue()
            assertThat(fakeEngine.createdConversations).isNotEmpty()
            assertThat(fakeEngine.createdConversations.all { it.closed }).isTrue()
        }

    /**
     * Minimal [Context] stand-in. The GPU backend path does not read from it.
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private class FakeContext : Context() {
        override fun getAssets(): AssetManager = throw NotImplementedError()

        override fun getResources(): Resources = throw NotImplementedError()

        override fun getPackageManager(): PackageManager = throw NotImplementedError()

        override fun getContentResolver(): ContentResolver = throw NotImplementedError()

        override fun getMainLooper(): Looper = throw NotImplementedError()

        override fun getApplicationContext(): Context = this

        override fun setTheme(resid: Int) = throw NotImplementedError()

        override fun getTheme(): Resources.Theme = throw NotImplementedError()

        override fun getClassLoader(): ClassLoader = this.javaClass.classLoader!!

        override fun getPackageName(): String = "dev.hossain.codematex.test"

        override fun getApplicationInfo(): ApplicationInfo = ApplicationInfo()

        override fun getPackageResourcePath(): String = throw NotImplementedError()

        override fun getPackageCodePath(): String = throw NotImplementedError()

        override fun getSharedPreferences(
            name: String,
            mode: Int,
        ): SharedPreferences = throw NotImplementedError()

        override fun moveSharedPreferencesFrom(
            sourceContext: Context,
            name: String,
        ): Boolean = throw NotImplementedError()

        override fun deleteSharedPreferences(name: String): Boolean = throw NotImplementedError()

        override fun openFileInput(name: String): FileInputStream = throw NotImplementedError()

        override fun openFileOutput(
            name: String,
            mode: Int,
        ): FileOutputStream = throw NotImplementedError()

        override fun deleteFile(name: String): Boolean = throw NotImplementedError()

        override fun getFileStreamPath(name: String): File = throw NotImplementedError()

        override fun getDataDir(): File = throw NotImplementedError()

        override fun getFilesDir(): File = throw NotImplementedError()

        override fun getNoBackupFilesDir(): File = throw NotImplementedError()

        override fun getExternalFilesDir(type: String?): File? = throw NotImplementedError()

        override fun getExternalFilesDirs(type: String?): Array<File> = throw NotImplementedError()

        override fun getObbDir(): File = throw NotImplementedError()

        override fun getObbDirs(): Array<File> = throw NotImplementedError()

        override fun getCacheDir(): File = throw NotImplementedError()

        override fun getCodeCacheDir(): File = throw NotImplementedError()

        override fun getExternalCacheDir(): File = throw NotImplementedError()

        override fun getExternalCacheDirs(): Array<File> = throw NotImplementedError()

        override fun getExternalMediaDirs(): Array<File> = throw NotImplementedError()

        override fun fileList(): Array<String> = throw NotImplementedError()

        override fun getDir(
            name: String,
            mode: Int,
        ): File = throw NotImplementedError()

        override fun getDatabasePath(name: String): File = throw NotImplementedError()

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
        ): SQLiteDatabase = throw NotImplementedError()

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?,
        ): SQLiteDatabase = throw NotImplementedError()

        override fun moveDatabaseFrom(
            sourceContext: Context,
            name: String,
        ): Boolean = throw NotImplementedError()

        override fun deleteDatabase(name: String): Boolean = throw NotImplementedError()

        override fun databaseList(): Array<String> = throw NotImplementedError()

        override fun getWallpaper(): Drawable = throw NotImplementedError()

        override fun peekWallpaper(): Drawable = throw NotImplementedError()

        override fun getWallpaperDesiredMinimumWidth(): Int = throw NotImplementedError()

        override fun getWallpaperDesiredMinimumHeight(): Int = throw NotImplementedError()

        override fun setWallpaper(bitmap: Bitmap?) = throw NotImplementedError()

        override fun setWallpaper(data: InputStream?) = throw NotImplementedError()

        override fun clearWallpaper() = throw NotImplementedError()

        override fun startActivity(intent: Intent) = throw NotImplementedError()

        override fun startActivity(
            intent: Intent,
            options: Bundle?,
        ) = throw NotImplementedError()

        override fun startActivities(intents: Array<Intent>) = throw NotImplementedError()

        override fun startActivities(
            intents: Array<Intent>,
            options: Bundle?,
        ) = throw NotImplementedError()

        override fun startIntentSender(
            intent: IntentSender?,
            fillInIntent: Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
        ) = throw NotImplementedError()

        override fun startIntentSender(
            intent: IntentSender?,
            fillInIntent: Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
            options: Bundle?,
        ) = throw NotImplementedError()

        override fun sendBroadcast(intent: Intent) = throw NotImplementedError()

        override fun sendBroadcast(
            intent: Intent,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcast(
            intent: Intent,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcast(
            intent: Intent,
            receiverPermission: String?,
            resultReceiver: BroadcastReceiver?,
            scheduler: Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: Bundle?,
        ) = throw NotImplementedError()

        override fun sendBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
        ) = throw NotImplementedError()

        override fun sendBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
            receiverPermission: String?,
            resultReceiver: BroadcastReceiver?,
            scheduler: Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: Bundle?,
        ) = throw NotImplementedError()

        override fun sendStickyBroadcast(intent: Intent) = throw NotImplementedError()

        override fun sendStickyOrderedBroadcast(
            intent: Intent,
            resultReceiver: BroadcastReceiver?,
            scheduler: Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: Bundle?,
        ) = throw NotImplementedError()

        override fun removeStickyBroadcast(intent: Intent) = throw NotImplementedError()

        override fun sendStickyBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
        ) = throw NotImplementedError()

        override fun sendStickyOrderedBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
            resultReceiver: BroadcastReceiver?,
            scheduler: Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: Bundle?,
        ) = throw NotImplementedError()

        override fun removeStickyBroadcastAsUser(
            intent: Intent,
            user: UserHandle,
        ) = throw NotImplementedError()

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
        ): Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            flags: Int,
        ): Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            broadcastPermission: String?,
            scheduler: Handler?,
        ): Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: BroadcastReceiver?,
            filter: IntentFilter?,
            broadcastPermission: String?,
            scheduler: Handler?,
            flags: Int,
        ): Intent? = throw NotImplementedError()

        override fun unregisterReceiver(receiver: BroadcastReceiver?) = throw NotImplementedError()

        override fun startService(service: Intent): ComponentName? = throw NotImplementedError()

        override fun stopService(name: Intent): Boolean = throw NotImplementedError()

        override fun startForegroundService(service: Intent): ComponentName? = throw NotImplementedError()

        override fun bindService(
            service: Intent,
            conn: ServiceConnection,
            flags: Int,
        ): Boolean = throw NotImplementedError()

        override fun unbindService(conn: ServiceConnection) = throw NotImplementedError()

        override fun startInstrumentation(
            className: ComponentName,
            profileFile: String?,
            arguments: Bundle?,
        ): Boolean = throw NotImplementedError()

        override fun getSystemService(name: String): Any? = throw NotImplementedError()

        override fun getSystemServiceName(serviceClass: Class<*>): String? = throw NotImplementedError()

        override fun checkPermission(
            permission: String,
            pid: Int,
            uid: Int,
        ): Int = throw NotImplementedError()

        override fun checkCallingPermission(permission: String): Int = throw NotImplementedError()

        override fun checkCallingOrSelfPermission(permission: String): Int = throw NotImplementedError()

        override fun checkSelfPermission(permission: String): Int = throw NotImplementedError()

        override fun enforcePermission(
            permission: String,
            pid: Int,
            uid: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingPermission(
            permission: String,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingOrSelfPermission(
            permission: String,
            message: String?,
        ) = throw NotImplementedError()

        override fun grantUriPermission(
            toPackage: String?,
            uri: Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun revokeUriPermission(
            uri: Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun revokeUriPermission(
            toPackage: String?,
            uri: Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun checkUriPermission(
            uri: Uri?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkUriPermission(
            uri: Uri?,
            permission: String?,
            callingUid: String?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkCallingUriPermission(
            uri: Uri?,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkCallingOrSelfUriPermission(
            uri: Uri?,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun enforceUriPermission(
            uri: Uri?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceUriPermission(
            uri: Uri?,
            permission: String?,
            callingUid: String?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingUriPermission(
            uri: Uri?,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingOrSelfUriPermission(
            uri: Uri?,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun createPackageContext(
            packageName: String,
            flags: Int,
        ): Context = throw NotImplementedError()

        override fun createContextForSplit(splitName: String): Context = throw NotImplementedError()

        override fun createConfigurationContext(overrideConfiguration: Configuration): Context = throw NotImplementedError()

        override fun createDisplayContext(display: Display): Context = throw NotImplementedError()

        override fun createDeviceProtectedStorageContext(): Context = throw NotImplementedError()

        override fun isDeviceProtectedStorage(): Boolean = false
    }
}
