package nota.android.crash.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
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

    private val bindLatch = CountDownLatch(1)
    private val bindLock = Any()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            broker = service
            bindLatch.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            broker = null
            fsManager = null
        }
    }

    private fun ensureBound(): IBinder? {
        if (broker == null) {
            try {
                RootService.bind(
                    Intent(context, CrashCenterRootService::class.java),
                    serviceConnection
                )
                bindLatch.await(15, TimeUnit.SECONDS)
            } catch (_: Exception) {
                // Root not available or binding failed
            }
        }
        return broker
    }

    private fun getFsManager(): FileSystemManager? {
        fsManager?.let { return it }
        synchronized(bindLock) {
            fsManager?.let { return it }
            val b = ensureBound() ?: return null
            return try {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(RootBroker.DESCRIPTOR)
                    b.transact(RootBroker.TRANSACTION_GET_FILE_SYSTEM_MANAGER, data, reply, 0)
                    reply.readException()
                    val fsBinder = reply.readStrongBinder()
                    FileSystemManager.getRemote(fsBinder).also { fsManager = it }
                } finally {
                    data.recycle()
                    reply.recycle()
                }
            } catch (_: Exception) {
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
        } catch (_: Exception) {
            RootAvailability.UNAVAILABLE
        }
    }

    override suspend fun readText(path: String): String? {
        val fsm = getFsManager() ?: return null
        return try {
            val channel = fsm.openChannel(path, FileSystemManager.MODE_READ_ONLY)
            Channels.newInputStream(channel).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun listDir(path: String): List<String> {
        val fsm = getFsManager() ?: return emptyList()
        return try {
            fsm.getFile(path).list()?.toList() ?: emptyList()
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun delete(path: String): Boolean {
        val fsm = getFsManager() ?: return false
        return try {
            fsm.getFile(path).delete()
        } catch (_: Exception) {
            false
        }
    }
}
