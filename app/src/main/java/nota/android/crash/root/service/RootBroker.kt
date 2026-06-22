package nota.android.crash.root.service

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import com.topjohnwu.superuser.nio.FileSystemManager

class RootBroker : Binder() {

    companion object {
        const val DESCRIPTOR = "nota.android.crash.root.service.RootBroker"
        const val TRANSACTION_GET_FILE_SYSTEM_MANAGER = FIRST_CALL_TRANSACTION + 1
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code == TRANSACTION_GET_FILE_SYSTEM_MANAGER) {
            data.enforceInterface(DESCRIPTOR)
            val fsBinder: IBinder = FileSystemManager.getService()
            reply!!.writeNoException()
            reply.writeStrongBinder(fsBinder)
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }
}
