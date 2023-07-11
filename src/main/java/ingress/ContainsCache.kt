package ingress

import printInfo
import structures.Article
import java.sql.Connection

class ContainsCache {
    private val keys = hashSetOf<String>()
    private val filo = mutableListOf<String>()
    private val capacity = 50_000

    fun fill(connection: Connection){
        val selectHashes = connection.prepareStatement("SELECT hash FROM articles ORDER BY created_at DESC LIMIT ?")
        selectHashes.setInt(1, capacity())
        val results = selectHashes.executeQuery()
        while(results.next()){
            insert(results.getString("hash"))
        }
        printInfo("ContainsCache", """Prefilled with ${size()} entries""")
    }

    fun capacity(): Int {
        return capacity
    }

    fun size(): Int {
        return keys.size
    }

    fun contains(value: String): Boolean {
        return keys.contains(value)
    }

    fun contains(value: Article): Boolean {
        return contains(value.normalized())
    }

    fun insert(value: String): Boolean {
        if (contains(value)) {
            return false // not inserted
        }

        keys.add(value)
        filo.add(value)

        if(keys.size > capacity){
            while(keys.size > capacity * 0.9 && filo.isNotEmpty()){
                keys.remove(filo.first())
                filo.removeFirst()
            }
        }

        return true // inserted
    }

    fun insert(value: Article): Boolean {
        return insert(value.normalized())
    }
}