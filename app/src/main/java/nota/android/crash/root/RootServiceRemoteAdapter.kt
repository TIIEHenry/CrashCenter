package nota.android.crash.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.withTimeout
import nota.android.crash.root.service.CrashCenterRootService
import nota.android.crash.root.service.RootBroker
import java.nio.channels.Channels
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RootServiceRemoteAdapter(private val context: Context) : RootAccessClient {

    @Volatile
    private var broker: IBinder? = null

    @Volatile
    private var fsManager: FileSystemManager? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val fsManagerLock = Any()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            broker = service
            connectLatch?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            broker = null
            fsManager = null
        }
    }

    @Volatile
    private var connectLatch: CountDownLatch? = null

    private fun ensureBound(): IBinder? {
        broker?.let { return it }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                if (broker == null) {
                    RootService.bind(
                        Intent(context, CrashCenterRootService::class.java),
                        serviceConnection,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "RootService.bind on main thread failed", e)
            }
            return broker
        }
        return try {
            val latch = CountDownLatch(1)
            connectLatch = latch
            mainHandler.post {
                try {
                    if (broker == null) {
                        RootService.bind(
                            Intent(context, CrashCenterRootService::class.java),
                            serviceConnection,
                        )
                    } else {
                        latch.countDown()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "RootService.bind failed", e)
                    latch.countDown()
                }
            }
            latch.await(15, TimeUnit.SECONDS)
            connectLatch = null
            broker
        } catch (e: Exception) {
            Log.w(TAG, "ensureBound failed", e)
            connectLatch = null
            null
        }
    }

    private fun getFsManager(): FileSystemManager? {
        fsManager?.let { return it }
        val boundBroker = ensureBound() ?: return null
        synchronized(fsManagerLock) {
            fsManager?.let { return it }
            return try {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(RootBroker.DESCRIPTOR)
                    boundBroker.transact(RootBroker.TRANSACTION_GET_FILE_SYSTEM_MANAGER, data, reply, 0)
                    reply.readException()
                    val fsBinder = reply.readStrongBinder()
                    FileSystemManager.getRemote(fsBinder).also { fsManager = it }
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            } catch (e: Exception) {
                Log.w(TAG, "getFsManager failed", e)
                null
            }
        }
    }

    override fun probe(): RootAvailability {
        return try {
            val shell = Shell.getShell()
            when {
                shell.isRoot -> RootAvailability.AVAILABLE
                else -> RootAvailability.DENIED
            }
        } catch (e: Exception) {
            Log.w(TAG, "probe failed", e)
            RootAvailability.UNAVAILABLE
        }
    }

    override suspend fun fileStat(path: String): RootFileStat? {
        val fsm = getFsManager() ?: return null
        return try {
            val file = fsm.getFile(path)
            if (!file.exists()) return null
            RootFileStat(mtimeMs = file.lastModified(), length = file.length())
        } catch (e: Exception) {
            Log.w(TAG, "fileStat failed", e)
            null
        }
    }

    override suspend fun readText(path: String): String? {
        val fsm = getFsManager() ?: return null
        return try {
            val channel = fsm.openChannel(path, FileSystemManager.MODE_READ_ONLY)
            Channels.newInputStream(channel).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "readText failed", e)
            null
        }
    }

    override suspend fun listDir(path: String): List<String> {
        val fsm = getFsManager() ?: return emptyList()
        return try {
            fsm.getFile(path).list()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "listDir failed", e)
            emptyList()
        }
    }

    override suspend fun appendBytes(path: String, data: ByteArray, deadlineMs: Long): Boolean {
        val fsm = getFsManager() ?: return false
        return try {
            withTimeout(deadlineMs) {
                val channel = fsm.openChannel(
                    path,
                    FileSystemManager.MODE_READ_WRITE or FileSystemManager.MODE_APPEND
                )
                Channels.newOutputStream(channel).use { it.write(data) }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "appendBytes failed", e)
            false
        }
    }

    override suspend fun writeText(path: String, content: String): Boolean {
        val fsm = getFsManager() ?: return false
        return try {
            val file = fsm.getFile(path)
            file.parentFile?.mkdirs()
            if (file.exists()) file.delete()
            Channels.newOutputStream(
                fsm.openChannel(path, FileSystemManager.MODE_WRITE_ONLY or FileSystemManager.MODE_CREATE),
            ).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "writeText failed", e)
            false
        }
    }

    override suspend fun delete(path: String): Boolean {
        val fsm = getFsManager() ?: return false
        return try {
            fsm.getFile(path).delete()
        } catch (e: Exception) {
            Log.w(TAG, "delete failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "RootServiceRemoteAdapter"
    }
}
