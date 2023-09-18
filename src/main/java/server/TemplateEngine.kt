package server

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter

class TemplateEngine() {
    private val velocityEngine = VelocityEngine()
    private var context = VelocityContext()
    private val charset = Charsets.UTF_8.name()

    init {
        velocityEngine.init()
        velocityEngine.setProperty("directive.parse.max_depth", "100")
        velocityEngine.setProperty("velocimacro.max_depth", "100")
        velocityEngine.setProperty("resource.default_encoding", charset)
        velocityEngine.setProperty("output.encoding", charset)
    }

    fun put(name: String, obj: Any){
        context.put(name, obj)
    }

    fun reset(){
        context = VelocityContext()
    }

    fun run(templateFile: String): String {
        val template: Template = velocityEngine.getTemplate(templateFile)
        template.encoding = charset

        val writer = StringWriter()
        template.merge(context, writer)

        return writer.toString()
    }
}