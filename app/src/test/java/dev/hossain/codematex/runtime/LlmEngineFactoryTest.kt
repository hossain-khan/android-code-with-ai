@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.EngineConfig
import dev.hossain.codematex.ui.overlay.ModelConfig
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

            assertTrue("Engine should be closed after conversation creation fails", fakeEngine.closed)
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
            assertEquals(false, fakeEngine.closed)
            assertEquals(false, fakeConversation.closed)
            assertEquals(LlmEngine.Backend.GPU, session.backend)
        }

    /**
     * Minimal [android.content.Context] stand-in. The GPU backend path does not read from it.
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private class FakeContext : android.content.Context() {
        override fun getAssets(): android.content.res.AssetManager = throw NotImplementedError()

        override fun getResources(): android.content.res.Resources = throw NotImplementedError()

        override fun getPackageManager(): android.content.pm.PackageManager = throw NotImplementedError()

        override fun getContentResolver(): android.content.ContentResolver = throw NotImplementedError()

        override fun getMainLooper(): android.os.Looper = throw NotImplementedError()

        override fun getApplicationContext(): android.content.Context = this

        override fun setTheme(resid: Int) = throw NotImplementedError()

        override fun getTheme(): android.content.res.Resources.Theme = throw NotImplementedError()

        override fun getClassLoader(): ClassLoader = this.javaClass.classLoader!!

        override fun getPackageName(): String = "dev.hossain.codematex.test"

        override fun getApplicationInfo(): android.content.pm.ApplicationInfo = android.content.pm.ApplicationInfo()

        override fun getPackageResourcePath(): String = throw NotImplementedError()

        override fun getPackageCodePath(): String = throw NotImplementedError()

        override fun getSharedPreferences(
            name: String,
            mode: Int,
        ): android.content.SharedPreferences = throw NotImplementedError()

        override fun moveSharedPreferencesFrom(
            sourceContext: android.content.Context,
            name: String,
        ): Boolean = throw NotImplementedError()

        override fun deleteSharedPreferences(name: String): Boolean = throw NotImplementedError()

        override fun openFileInput(name: String): java.io.FileInputStream = throw NotImplementedError()

        override fun openFileOutput(
            name: String,
            mode: Int,
        ): java.io.FileOutputStream = throw NotImplementedError()

        override fun deleteFile(name: String): Boolean = throw NotImplementedError()

        override fun getFileStreamPath(name: String): java.io.File = throw NotImplementedError()

        override fun getDataDir(): java.io.File = throw NotImplementedError()

        override fun getFilesDir(): java.io.File = throw NotImplementedError()

        override fun getNoBackupFilesDir(): java.io.File = throw NotImplementedError()

        override fun getExternalFilesDir(type: String?): java.io.File? = throw NotImplementedError()

        override fun getExternalFilesDirs(type: String?): Array<java.io.File> = throw NotImplementedError()

        override fun getObbDir(): java.io.File = throw NotImplementedError()

        override fun getObbDirs(): Array<java.io.File> = throw NotImplementedError()

        override fun getCacheDir(): java.io.File = throw NotImplementedError()

        override fun getCodeCacheDir(): java.io.File = throw NotImplementedError()

        override fun getExternalCacheDir(): java.io.File = throw NotImplementedError()

        override fun getExternalCacheDirs(): Array<java.io.File> = throw NotImplementedError()

        override fun getExternalMediaDirs(): Array<java.io.File> = throw NotImplementedError()

        override fun fileList(): Array<String> = throw NotImplementedError()

        override fun getDir(
            name: String,
            mode: Int,
        ): java.io.File = throw NotImplementedError()

        override fun getDatabasePath(name: String): java.io.File = throw NotImplementedError()

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: android.database.sqlite.SQLiteDatabase.CursorFactory?,
        ): android.database.sqlite.SQLiteDatabase = throw NotImplementedError()

        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: android.database.sqlite.SQLiteDatabase.CursorFactory?,
            errorHandler: android.database.DatabaseErrorHandler?,
        ): android.database.sqlite.SQLiteDatabase = throw NotImplementedError()

        override fun moveDatabaseFrom(
            sourceContext: android.content.Context,
            name: String,
        ): Boolean = throw NotImplementedError()

        override fun deleteDatabase(name: String): Boolean = throw NotImplementedError()

        override fun databaseList(): Array<String> = throw NotImplementedError()

        override fun getWallpaper(): android.graphics.drawable.Drawable = throw NotImplementedError()

        override fun peekWallpaper(): android.graphics.drawable.Drawable = throw NotImplementedError()

        override fun getWallpaperDesiredMinimumWidth(): Int = throw NotImplementedError()

        override fun getWallpaperDesiredMinimumHeight(): Int = throw NotImplementedError()

        override fun setWallpaper(bitmap: android.graphics.Bitmap?) = throw NotImplementedError()

        override fun setWallpaper(data: java.io.InputStream?) = throw NotImplementedError()

        override fun clearWallpaper() = throw NotImplementedError()

        override fun startActivity(intent: android.content.Intent) = throw NotImplementedError()

        override fun startActivity(
            intent: android.content.Intent,
            options: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun startActivities(intents: Array<android.content.Intent>) = throw NotImplementedError()

        override fun startActivities(
            intents: Array<android.content.Intent>,
            options: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun startIntentSender(
            intent: android.content.IntentSender?,
            fillInIntent: android.content.Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
        ) = throw NotImplementedError()

        override fun startIntentSender(
            intent: android.content.IntentSender?,
            fillInIntent: android.content.Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
            options: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun sendBroadcast(intent: android.content.Intent) = throw NotImplementedError()

        override fun sendBroadcast(
            intent: android.content.Intent,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcast(
            intent: android.content.Intent,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcast(
            intent: android.content.Intent,
            receiverPermission: String?,
            resultReceiver: android.content.BroadcastReceiver?,
            scheduler: android.os.Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun sendBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
        ) = throw NotImplementedError()

        override fun sendBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
            receiverPermission: String?,
        ) = throw NotImplementedError()

        override fun sendOrderedBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
            receiverPermission: String?,
            resultReceiver: android.content.BroadcastReceiver?,
            scheduler: android.os.Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun sendStickyBroadcast(intent: android.content.Intent) = throw NotImplementedError()

        override fun sendStickyOrderedBroadcast(
            intent: android.content.Intent,
            resultReceiver: android.content.BroadcastReceiver?,
            scheduler: android.os.Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun removeStickyBroadcast(intent: android.content.Intent) = throw NotImplementedError()

        override fun sendStickyBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
        ) = throw NotImplementedError()

        override fun sendStickyOrderedBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
            resultReceiver: android.content.BroadcastReceiver?,
            scheduler: android.os.Handler?,
            initialCode: Int,
            initialData: String?,
            initialExtras: android.os.Bundle?,
        ) = throw NotImplementedError()

        override fun removeStickyBroadcastAsUser(
            intent: android.content.Intent,
            user: android.os.UserHandle,
        ) = throw NotImplementedError()

        override fun registerReceiver(
            receiver: android.content.BroadcastReceiver?,
            filter: android.content.IntentFilter?,
        ): android.content.Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: android.content.BroadcastReceiver?,
            filter: android.content.IntentFilter?,
            flags: Int,
        ): android.content.Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: android.content.BroadcastReceiver?,
            filter: android.content.IntentFilter?,
            broadcastPermission: String?,
            scheduler: android.os.Handler?,
        ): android.content.Intent? = throw NotImplementedError()

        override fun registerReceiver(
            receiver: android.content.BroadcastReceiver?,
            filter: android.content.IntentFilter?,
            broadcastPermission: String?,
            scheduler: android.os.Handler?,
            flags: Int,
        ): android.content.Intent? = throw NotImplementedError()

        override fun unregisterReceiver(receiver: android.content.BroadcastReceiver?) = throw NotImplementedError()

        override fun startService(service: android.content.Intent): android.content.ComponentName? = throw NotImplementedError()

        override fun stopService(name: android.content.Intent): Boolean = throw NotImplementedError()

        override fun startForegroundService(service: android.content.Intent): android.content.ComponentName? = throw NotImplementedError()

        override fun bindService(
            service: android.content.Intent,
            conn: android.content.ServiceConnection,
            flags: Int,
        ): Boolean = throw NotImplementedError()

        override fun unbindService(conn: android.content.ServiceConnection) = throw NotImplementedError()

        override fun startInstrumentation(
            className: android.content.ComponentName,
            profileFile: String?,
            arguments: android.os.Bundle?,
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
            uri: android.net.Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun revokeUriPermission(
            uri: android.net.Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun revokeUriPermission(
            toPackage: String?,
            uri: android.net.Uri?,
            modeFlags: Int,
        ) = throw NotImplementedError()

        override fun checkUriPermission(
            uri: android.net.Uri?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkUriPermission(
            uri: android.net.Uri?,
            permission: String?,
            callingUid: String?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkCallingUriPermission(
            uri: android.net.Uri?,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun checkCallingOrSelfUriPermission(
            uri: android.net.Uri?,
            modeFlags: Int,
        ): Int = throw NotImplementedError()

        override fun enforceUriPermission(
            uri: android.net.Uri?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceUriPermission(
            uri: android.net.Uri?,
            permission: String?,
            callingUid: String?,
            pid: Int,
            uid: Int,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingUriPermission(
            uri: android.net.Uri?,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun enforceCallingOrSelfUriPermission(
            uri: android.net.Uri?,
            modeFlags: Int,
            message: String?,
        ) = throw NotImplementedError()

        override fun createPackageContext(
            packageName: String,
            flags: Int,
        ): android.content.Context = throw NotImplementedError()

        override fun createContextForSplit(splitName: String): android.content.Context = throw NotImplementedError()

        override fun createConfigurationContext(overrideConfiguration: android.content.res.Configuration): android.content.Context =
            throw NotImplementedError()

        override fun createDisplayContext(display: android.view.Display): android.content.Context = throw NotImplementedError()

        override fun createDeviceProtectedStorageContext(): android.content.Context = throw NotImplementedError()

        override fun isDeviceProtectedStorage(): Boolean = false
    }
}
