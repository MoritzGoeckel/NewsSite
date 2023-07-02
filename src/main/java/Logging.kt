import org.slf4j.LoggerFactory

fun printInfo(location: String, message: String){
    LoggerFactory.getLogger(location).info(message)
}

fun printWarning(location: String, message: String){
    LoggerFactory.getLogger(location).warn(message)
}

fun printError(location: String, message: String){
    LoggerFactory.getLogger(location).error(message)
}

fun printTrace(location: String, message: String){
    LoggerFactory.getLogger(location).trace(message)
}