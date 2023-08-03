package me.mason.client.legacy//package me.mason.client
//
//import org.lwjgl.glfw.GLFW
//import java.util.*
//import kotlin.collections.ArrayList
//
//interface Input {
//    val keyEvent: ArrayList<(Int, Int) -> (Unit)>
//    val mouseEvent: ArrayList<(Int, Int) -> (Unit)>
//    val keys: BitSet
//    val buttons: BitSet
//}
//
//fun Window.input(block: Input.() -> (Unit)) {
//    val keys = BitSet(128)
//    val buttons = BitSet(128)
//    val input = object : Input {
//        override val keyEvent = ArrayList<(Int, Int) -> Unit>()
//        override val mouseEvent = ArrayList<(Int, Int) -> Unit>()
//        override val keys = keys
//        override val buttons = buttons
//    }
//    GLFW.glfwSetKeyCallback(window) { _, code, _, action, _ ->
//        input.keyEvent.forEach { it(code, action) }
//        if (action == GLFW.GLFW_PRESS) keys.set(code)
//        else if (action == GLFW.GLFW_RELEASE) keys.clear(code)
//    }
//    GLFW.glfwSetMouseButtonCallback(window) { _, code, action, _ ->
//        input.mouseEvent.forEach { it(code, action) }
//        if (action == GLFW.GLFW_PRESS) buttons.set(code)
//        else if (action == GLFW.GLFW_RELEASE) buttons.clear(code)
//    }
//    block(input)
//}