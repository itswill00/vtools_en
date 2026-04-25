package com.omarea.vtools.utils

import com.omarea.common.shell.KeepShellPublic
import java.util.ArrayList

data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val isEnabled: Boolean,
    val isRemoved: Boolean,
    val path: String
)

class ModuleUtils {
    companion object {
        private const val MODULES_DIR = "/data/adb/modules"

        fun getInstalledModules(): List<ModuleInfo> {
            val modules = ArrayList<ModuleInfo>()
            val folders = KeepShellPublic.doCmdSync("ls $MODULES_DIR").split("\n")

            for (id in folders) {
                if (id.trim().isEmpty()) continue
                
                val path = "$MODULES_DIR/$id"
                val propPath = "$path/module.prop"
                
                // Read module.prop content
                val propContent = KeepShellPublic.doCmdSync("cat $propPath")
                if (propContent == "error" || propContent.isEmpty()) continue

                val props = parseModuleProp(propContent)
                
                // Check status
                val isDisabled = KeepShellPublic.doCmdSync("if [ -f $path/disable ]; then echo 'true'; else echo 'false'; fi") == "true"
                val isRemoved = KeepShellPublic.doCmdSync("if [ -f $path/remove ]; then echo 'true'; else echo 'false'; fi") == "true"

                modules.add(ModuleInfo(
                    id = id,
                    name = props["name"] ?: id,
                    version = props["version"] ?: "Unknown",
                    author = props["author"] ?: "Unknown",
                    description = props["description"] ?: "",
                    isEnabled = !isDisabled,
                    isRemoved = isRemoved,
                    path = path
                ))
            }
            return modules
        }

        private fun parseModuleProp(content: String): Map<String, String> {
            val map = HashMap<String, String>()
            content.split("\n").forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
            return map
        }
        
        fun toggleModule(path: String, enable: Boolean) {
            if (enable) {
                KeepShellPublic.doCmdSync("rm $path/disable")
            } else {
                KeepShellPublic.doCmdSync("touch $path/disable")
            }
        }
        
        fun removeModule(path: String) {
            KeepShellPublic.doCmdSync("touch $path/remove")
        }
    }
}
