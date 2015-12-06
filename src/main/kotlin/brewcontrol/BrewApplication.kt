package brewcontrol

import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.setup.Environment
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

fun main(args: Array<String>) {
    println("Hi from Kotlin")
    BrewApplication().run(*args)
}

class BrewConfiguration : Configuration()

class BrewApplication : Application<BrewConfiguration>() {
    override fun run(configuration: BrewConfiguration?, environment: Environment?) {
        println("Hi from BrewApplication")
        checkNotNull(environment).jersey().register(WebResource::class.java)
    }
}
