package server

import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter


class FrontPage {
    fun html(): String {

        val velocityEngine = VelocityEngine()
        velocityEngine.init()

        val template: Template = velocityEngine.getTemplate("static/templates/index.vm")

        val context = VelocityContext()
        context.put("name", "Moritz")

        val writer = StringWriter()
        template.merge(context, writer)

        return writer.toString()
    }
}