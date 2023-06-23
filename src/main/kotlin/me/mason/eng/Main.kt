package me.mason.eng
//
import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW.*
import java.util.BitSet

//
////
//val TILE_UV_SIZE = 4
////val TILE_UV_SIZE_VEC = vec(4, 4)
//val TILES = Array(11) {
//    val x = (it * 4) % 512
//    val y = ((it * 4) / 512) * 4
//    vec(x, y)
//}
//val SOLIDS = arrayOf(0, 1)
//val SELECTED = vec(4, 4)
//val SELECTED_UV_SIZE = 4
//val SELECTED_UV_SIZE_VEC = vec(4, 4)
//
//val CROSSHAIR = vec(0, 9)
//val CROSSHAIR_SIZE_VEC = vec(10f, 10f)
//val CROSSHAIR_UV_SIZE = 5
//val CROSSHAIR_UV_SIZE_VEC = vec(5, 5)
//
//val PLAYER = vec(0, 4)
//val PLAYER_UV_SIZE = 4
//val PLAYER_UV_SIZE_VEC = vec(4, 4)
//
//val TILE_SIZE = 16f
//val TILE_SIZE_VEC = vec(16f, 16f)
//
//
//val TILE_PADDED = TILE_SIZE * 1.25f
////
//interface Input {
//    val key: ArrayList<(Int, Int) -> (Unit)>
//    val button: ArrayList<(Int, Int) -> (Unit)>
//    val keys: BitSet
//    val buttons: BitSet
//}
//
//fun Window.input(block: Input.() -> (Unit)) {
//    val keys = BitSet(128)
//    val buttons = BitSet(128)
//    val input = object : Input {
//        override val key = ArrayList<(Int, Int) -> Unit>()
//        override val button = ArrayList<(Int, Int) -> Unit>()
//        override val keys = keys
//        override val buttons = buttons
//    }
//    glfwSetKeyCallback(window) { _, code, _, action, _ ->
//        input.key.forEach { it(code, action) }
//        if (action == GLFW_PRESS) keys.set(code)
//        else if (action == GLFW_RELEASE) keys.clear(code)
//    }
//    glfwSetMouseButtonCallback(window) { _, code, action, _ ->
//        input.button.forEach { it(code, action) }
//        if (action == GLFW_PRESS) buttons.set(code)
//        else if (action == GLFW_RELEASE) buttons.clear(code)
//    }
//    block(input)
//}
//
//const val WORLD_SIZE = 1024
//
//fun Window.mouseScreen(camera: FloatVector): FloatVector {
//    val mouseX = BufferUtils.createDoubleBuffer(1)
//    val mouseY = BufferUtils.createDoubleBuffer(1)
//    val width = BufferUtils.createIntBuffer(1)
//    val height = BufferUtils.createIntBuffer(1)
//    glfwGetCursorPos(id, mouseX, mouseY)
//    glfwGetWindowSize(id, width, height)
//    return vec(mouseX.get().toFloat() - 640 + camera.x, height.get() - mouseY.get().toFloat() - 360 + camera.y)
//}
//
//fun Window.mouseWorld(camera: FloatVector): FloatVector {
//    val mouseX = BufferUtils.createDoubleBuffer(1)
//    val mouseY = BufferUtils.createDoubleBuffer(1)
//    val width = BufferUtils.createIntBuffer(1)
//    val height = BufferUtils.createIntBuffer(1)
//    glfwGetCursorPos(id, mouseX, mouseY)
//    glfwGetWindowSize(id, width, height)
//    return vec(mouseX.get().toFloat() + camera.x + (WORLD_SIZE / 2) * TILE_SIZE - 640, height.get() - mouseY.get().toFloat() + camera.y + (WORLD_SIZE / 2) * TILE_SIZE - 360)
//}
//
//fun FloatVector.worldToTile() = ((this / TILE_SIZE) + WORLD_SIZE / 2f).rounded().int()
//fun IntVector.tileToWorld() = (this - WORLD_SIZE / 2).float() * TILE_SIZE
//
//fun IntArray.tiledRaycast(
//    start: IntVector,
//    dir: FloatVector
//): FloatVector {
//    val stepSize =
//        if (dir.y == 0f) vec(1f, 0f)
//        else if (dir.x == 0f) vec(0f, 1f)
//        else vec(sqrt(1 + (dir.y / dir.x) * (dir.y / dir.x)), sqrt(1 + (dir.x / dir.y) * (dir.x / dir.y)))
//    val current = start.clone()
//    val length = vec(0f, 0f)
//    val step = vec(0, 0)
//    if (dir.x < 0) { length.x = (start.x - current.x.toFloat()) * stepSize.x; step.x = -1 }
//    else { length.x = (current.x.toFloat() - start.x) * stepSize.x; step.x = 1 }
//    if (dir.y < 0) { length.y = (start.y - current.y.toFloat()) * stepSize.y; step.y = -1 }
//    else { length.y = (current.y.toFloat() - start.y) * stepSize.y; step.y = 1 }
//
//    val max = 100f
//    var found = false
//    var walked = 0f
//    while(!found && walked < max) {
//        if (length.x < length.y) {
//            current.x += step.x
//            walked = length.x
//            length.x += stepSize.x
//        } else {
//            current.y += step.y
//            walked = length.y
//            length.y += stepSize.y
//        }
//        val tile = this[current.x + current.y * WORLD_SIZE]
//        found = (current.x < 0 || current.x > WORLD_SIZE || current.y < 0 || current.y > WORLD_SIZE) || tile in SOLIDS
//    }
//    println("a: ${start.float()}")
//    println("b: ${dir}")
//    println("c: ${dir * walked}")
//    println("d: ${(start.float() + dir * walked) * TILE_SIZE}")
//    return ((start.float() + dir * walked) * TILE_SIZE).rounded()
//}
//
//fun main() = window { input {
//    title = "title"
//    width = 1280
//    height = 720
//    val texture = shader("texture.vert", "texture.frag", 2, 1)
//    val camera = object : Camera {
//        override val projection = Matrix4f().setOrtho(-640f, 640f, -360f, 360f, 0f, 100f)
//        override val view = Matrix4f().identity().lookAt(
//            Vector3f(0f, 0f, 20f),
//            Vector3f(0f, 0f, -1f),
//            Vector3f(0f, 1f, 0f)
//        )
//        override var position = fvec()
//        override fun move() {
//            view.identity().lookAt(
//                Vector3f(round(position.x), round(position.y), 20f),
//                Vector3f(round(position.x), round(position.y), -1f),
//                Vector3f(0f, 1f, 0f)
//            )
//        }
//    }
//    var selected = 0
//    val render = 86
//    val world = IntArray(WORLD_SIZE * WORLD_SIZE) { 2 }
//    val bytes = Paths.get("world.tiles").readBytes()
//    ByteBuffer.wrap(bytes).apply {
//        if (bytes.isNotEmpty())
//            for (i in 0 until WORLD_SIZE * WORLD_SIZE) world[i] = getInt()
//    }
//
//    glfwSetScrollCallback(id) { _, x, y ->
//        if (y < 0) selected = min(selected + 1, TILES.size - 1)
//        else if (y > 0) selected = max(selected - 1, 0)
//    }
//    val intersect = vec(0f, 0f)
//    button += button@{ key, action ->
//        if (key != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS) return@button
//        val mouseTile = (mouseWorld(camera.position) / TILE_SIZE).rounded().int()
//        val cameraTile = ((camera.position + (WORLD_SIZE / 2) * TILE_SIZE) / TILE_SIZE).floored().int()
//        val theta = atan2((mouseTile.y - cameraTile.y).toDouble(), (mouseTile.x - cameraTile.x).toDouble())
//        intersect.set(world.tiledRaycast(cameraTile, vec(cos(theta).toFloat(), sin(theta).toFloat())))
//        println(mouseWorld(camera.position))
//    }
//    key += key@ { key, action ->
//        if (key != GLFW_KEY_S || action != GLFW_PRESS || (!keys[GLFW_KEY_LEFT_SHIFT] && !keys[GLFW_KEY_RIGHT_SHIFT])) return@key
//        println("save")
//        Paths.get("world.tiles").writeBytes(allocate(WORLD_SIZE * WORLD_SIZE * Int.SIZE_BYTES).apply {
//            world.forEach { putInt(it) }
//        }.array())
//    }
//    buffer(texture, 512, Paths.get("shooter.png"), camera, fixed(128) { delta, elapsed, fixedTick ->
//        if (fixedTick) {
//            var x = 0
//            var y = 0
//            if (keys[GLFW_KEY_W]) y++
//            if (keys[GLFW_KEY_S]) y--
//            if (keys[GLFW_KEY_A]) x--
//            if (keys[GLFW_KEY_D]) x++
//            val theta = atan2(y.toDouble(), x.toDouble())
//            if (x != 0 || y != 0) {
//                camera.position.x += cos(theta).toFloat() * 2f
//                camera.position.y += sin(theta).toFloat() * 2f
//                camera.move()
//            }
//        }
//        val cameraTile = (camera.position / TILE_SIZE).floored().int()
//        for (i in 0 until render * render) {
//            val tx = i % render - render / 2 + cameraTile.x
//            val ty = i / render - render / 2 + cameraTile.y
//            val tile = (tx + WORLD_SIZE / 2) + (ty + WORLD_SIZE / 2) * WORLD_SIZE
//            if (tx < -WORLD_SIZE / 2 || tx >= WORLD_SIZE / 2 || ty < -WORLD_SIZE / 2 || ty >= WORLD_SIZE / 2) continue
//            if (world[tile] == -1) continue
//            quad(
//                i,
//                vec(tx * TILE_SIZE, ty * TILE_SIZE),
//                TILE_SIZE_VEC,
//                TILES[world[tile]],
//                SELECTED_UV_SIZE_VEC
//            )
//        }
//        for (i in TILES.indices) {
//            val tile = TILES[i]
//            quad(render * render + i,
//                (vec(i * TILE_PADDED + TILE_PADDED / 2 - 640f, -TILE_PADDED / 2 + 360f) + camera.position),
//                TILE_SIZE_VEC,
//                tile,
//                TILE_UV_SIZE_VEC
//            )
//        }
//        quad(render * render + TILES.size,
//            (vec(selected * TILE_PADDED + TILE_PADDED / 2 - 640f, -TILE_PADDED / 2 + 360f) + camera.position),
//            TILE_SIZE_VEC,
//            SELECTED,
//            SELECTED_UV_SIZE_VEC
//        )
//        quad(render * render + TILES.size + 1,
//            camera.position,
//            TILE_SIZE_VEC,
//            PLAYER,
//            PLAYER_UV_SIZE_VEC
//        )
//        val cursorScreen = mouseScreen(camera.position)
//        val angle = atan2(camera.position.y - cursorScreen.y, camera.position.x - cursorScreen.x) + Math.PI.toFloat()
//        val x = cos(angle) * 30.0f
//        val y = sin(angle) * 30.0f
//        quad(render * render + TILES.size + 2,
//            camera.position + vec(x, y),
//            CROSSHAIR_SIZE_VEC,
//            CROSSHAIR,
//            CROSSHAIR_UV_SIZE_VEC
//        )
//        quad(render * render + TILES.size + 3,
//            cursorScreen,
//            CROSSHAIR_SIZE_VEC,
//            CROSSHAIR,
//            CROSSHAIR_UV_SIZE_VEC
//        )
////        println("intersect: ${intersect - vec(WORLD_SIZE / 2, WORLD_SIZE / 2).float() * TILE_SIZE}")
//        quad(render * render + TILES.size + 4,
//            intersect - vec(WORLD_SIZE / 2, WORLD_SIZE / 2).float() * TILE_SIZE,
//            CROSSHAIR_SIZE_VEC,
//            CROSSHAIR,
//            CROSSHAIR_UV_SIZE_VEC
//        )
//        if (buttons[GLFW_MOUSE_BUTTON_1]) {
//            val rounded = (mouseWorld(camera.position) / TILE_SIZE).rounded().int()
//            val tile = rounded.x + rounded.y * WORLD_SIZE
//            world[tile] = selected
//        }
//    })
//} }
