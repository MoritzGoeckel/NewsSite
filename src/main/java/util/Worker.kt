package util

import kotlin.concurrent.thread

class Worker(private val fn: () -> Unit){
    private var interrupted: Boolean = false
    private var thread: Thread? = null

    fun interrupt(){
        interrupted = true
    }

    fun start(): Worker {
        this.thread = thread(start = true) {
            while (!interrupted) {
                try {
                    fn()
                } catch (e: Exception) {
                    printError("Worker", e.toString())
                    e.printStackTrace()
                }
            }
        }
        return this
    }
}