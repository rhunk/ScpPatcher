package me.rhunk.scppatcher

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.topjohnwu.superuser.Shell
import me.rhunk.scppatcher.protobuf.ProtoEditor
import me.rhunk.scppatcher.protobuf.ProtoReader
import kotlin.time.Duration.Companion.days

class CorePatcher(
    private val context: Context,
) {
    companion object {
        init {
            Shell.enableVerboseLogging = true
        }
    }

    private fun openCoreDatabase(block: SQLiteDatabase.() -> Unit) {
        val cacheFolder = context.cacheDir

        val pid = Shell.cmd("pidof com.snapchat.android").exec().out.singleOrNull()?.toIntOrNull() ?: throw Exception("Failed to get snapchat pid. Make sure snapchat is running!")
        Log.d("CorePatcher", "Snapchat PID: $pid")

        val fileDescriptors = Shell.cmd("ls -l /proc/$pid/fd").exec().out
        val files = mutableMapOf<String, Int>()

        fileDescriptors.forEach { line ->
            if (line.contains("/core.db")) {
                val fd = line.substringBefore(" -> ").substringAfterLast(" ").toIntOrNull() ?: return@forEach
                files[line.substringAfter(" -> ")] = fd
            }
        }

        files.forEach { (filePath, fd) ->
            val filename = filePath.substringAfterLast("/");

            Shell.cmd("cp /proc/$pid/fd/$fd ${cacheFolder.absolutePath}/$filename").exec().apply {
                if (!isSuccess) {
                    throw Exception("Failed to copy $filePath")
                }
            }

            Shell.cmd("chmod 777 ${cacheFolder.absolutePath}/$filename").exec().apply {
                if (!isSuccess) {
                    throw Exception("Failed to chmod $filePath")
                }
            }
        }

        SQLiteDatabase.openDatabase(cacheFolder.absolutePath + "/core.db", null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING).use {
            it.block()
        }

        // copy back to the original location
        Shell.cmd("cp " + cacheFolder.absolutePath + "/core.db /proc/$pid/fd/${files.entries.first { (key, _) -> key.endsWith("core.db") }.value}").exec().apply {
            if (!isSuccess) {
                throw Exception("Failed to copy back to the original location")
            }
        }

        cacheFolder.listFiles()?.filter { it.name.contains("core.db") }?.forEach { it.delete() }

        // clean journal files
        files.entries.filter { (key, _) -> !key.endsWith("core.db") }.forEach { (key, value) ->
            Shell.cmd(":> /proc/$pid/fd/$value").exec().apply {
                if (!isSuccess) {
                    throw Exception("Failed to remove journal file $key")
                }
            }
        }
    }

    fun patchCore() {
        openCoreDatabase {
            val blobContent = rawQuery("SELECT blobVal FROM SnapUserStore WHERE _id = 25", null).use {
                it.moveToNext()
                it.getBlob(0)
            }

            val patchedBlob = ProtoEditor(blobContent).apply {
                edit {
                    editEach(2) {
                        val key = (firstOrNull(1)?.value as? ByteArray)?.toString(Charsets.UTF_8)
                        Log.d("CorePatcher", "found key $key")
                        if (key == "25") {
                            remove(2)
                            add(2) {
                                from(3) {
                                    addVarInt(1, 2) // SNAPCHAT_PLUS_AD_FREE
                                    addVarInt(2, 1)
                                    addVarInt(3, System.currentTimeMillis() - 15.days.inWholeMilliseconds)
                                    addVarInt(4, System.currentTimeMillis() + 380.days.inWholeMilliseconds)
                                    addVarInt(6, 1)
                                }
                            }
                            Log.d("CorePatcher", "injected PlusSubscriptionInfo")
                        }
                    }
                }
            }.toByteArray()

            // update the database
            execSQL("UPDATE SnapUserStore SET blobVal = ? WHERE _id = 25", arrayOf(patchedBlob))

            Log.d("CorePatcher", "Patched blob ${ProtoReader(patchedBlob)}")
        }

        // kill snapchat
        Shell.cmd("am force-stop com.snapchat.android").exec().apply {
            if (!isSuccess) {
                throw Exception("Failed to kill snapchat. Make sure you've allowed root access!")
            }
        }
    }
}