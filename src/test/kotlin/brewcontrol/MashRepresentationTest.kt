package brewcontrol

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MashRepresentationTest {
    val om = createObjectMapper()

    private val heatTaskJson = "{\"type\":\"HeatTask\",\"temperature\":50.0}"
    private val recipeJson = "{\"activeTaskIndex\":0,\"tasks\":[{\"type\":\"HeatTask\",\"temperature\":50.0}]}"

    @Test
    fun serializesHeatTask() {
        val heatTask = HeatTask(50.0)
        assertThat(om.readTree(om.writeValueAsString(heatTask))).isEqualTo(om.readTree(heatTaskJson));
    }

    @Test
    fun deserializesHeatTask() {
        assertThat(om.readValue(heatTaskJson, HeatTask::class.java)).isEqualTo(HeatTask(50.0))
    }

    @Test
    fun deserializesTasksPolymorphic() {
        assertThat(om.readValue(heatTaskJson, Task::class.java)).isEqualTo(HeatTask(50.0))
    }

    @Test
    fun serializesRecipe() {
        val recipe = Recipe(0, listOf(HeatTask(50.0)))
        assertThat(om.readTree(om.writeValueAsString(recipe))).isEqualTo(om.readTree(recipeJson));
    }
}