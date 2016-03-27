package brewcontrol

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

val om = objectMapper

class StepsRepresentationTest {
    private val heatStepJson = "{\"type\":\"Heat\",\"temperature\":50.0}"

    @Test
    fun serializesHeatStep() {
        val heatStep = HeatStep(50.0)
        assertThat(om.readTree(om.writeValueAsString(heatStep))).isEqualTo(om.readTree(heatStepJson));
    }

    @Test
    fun deserializesHeatStep() {
        assertThat(om.readValue(heatStepJson, HeatStep::class.java)).isEqualTo(HeatStep(50.0))
    }

    @Test
    fun deserializesStepsPolymorphic() {
        assertThat(om.readValue(heatStepJson, Step::class.java)).isEqualTo(HeatStep(50.0))
    }
}

class RecipeRepresentationTest {
    private val recipeJson = "{\"steps\":[{\"type\":\"Heat\",\"temperature\":50.0}]}"
    @Test
    fun serializes() {
        val recipe = Recipe(listOf(HeatStep(50.0)))
        assertThat(om.readTree(om.writeValueAsString(recipe))).isEqualTo(om.readTree(recipeJson));
    }

    @Test
    fun defaultRecipeReadWrite() {
        val recipe = brewcontrol.Recipe()
        val recipe2 = om.readValue(om.writeValueAsString(recipe), Recipe::class.java)
        assertThat(recipe2).isEqualTo(recipe)
    }
}

class RecipeProcessRepresentationTest {
    val recipeProcessJson = "{\"activeTaskIndex\":-1,\"tasks\":[{\"step\":{\"type\":\"Heat\",\"temperature\":10.0}}]}"

    @Test
    fun serializes() {
        val recipeProcess = RecipeProcess(Recipe(listOf(HeatStep(10.0))))
        assertThat(om.readTree(om.writeValueAsString(recipeProcess))).isEqualTo(om.readTree(recipeProcessJson))
    }
}