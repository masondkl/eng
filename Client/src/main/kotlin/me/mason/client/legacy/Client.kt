package me.mason.client.legacy//package me.mason.client
//
//import com.github.exerosis.mynt.SocketProvider
//import com.github.exerosis.mynt.base.Address
//import com.github.exerosis.mynt.base.Write
//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
//import kotlinx.coroutines.channels.consumeEach
//import me.mason.shared.*
//import org.joml.Vector2f
//import org.joml.Vector2i
//import org.joml.Vector3f
//import org.joml.Vector4f
//import org.lwjgl.BufferUtils
//import org.lwjgl.glfw.GLFW.*
//import org.lwjgl.opengl.GL20.*
//import org.lwjgl.opengl.GL30.glBindVertexArray
//import java.lang.Math.toRadians
//import java.nio.channels.AsynchronousChannelGroup
//import java.nio.channels.ClosedChannelException
//import java.nio.file.Paths
//import java.util.*
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.Executors
//import kotlin.collections.ArrayList
//import kotlin.math.PI
//import kotlin.math.atan2
//import kotlin.math.cos
//import kotlin.math.sin
//
//
//const val COLLISION_SIZE = 4
//const val COLLISION_RADIUS = COLLISION_SIZE / 2
//
//val BOMB_FRAMES = Array(4) { Vector2i(it * 4, 496) }
//val DEFUSE_MARK = Vector2i(0, 458)
//val DEFUSE_MARK_UV_SIZE = Vector2i(38, 38)
//val DEFUSE_MARK_SIZE = Vector2f(9.5f, 9.5f)
//
//val COUNTER_PLAYER = Vector2i(0, 500)
//val TERRORIST_PLAYER = Vector2i(4, 500)
//val DEAD_PLAYER = Vector2i(8, 500)
//val PLAYER_UV_DIM = Vector2i(4, 4)
//val SELECTED = Vector2i(11, 504)
//
//val RED_SPAWN_MARKER = Vector2i(15, 504)
//val BLUE_SPAWN_MARKER = Vector2i(19, 504)
//
//val BULLET = Vector2i(9, 506)
//val BULLET_UV_DIM = Vector2i(1, 1)
//val BULLET_DIM = Vector2f(0.5f, 0.5f)
//
//val GUN = Vector2i(0, 450)
//val GUN_FLIPPED = Vector2i(8, 450)
//val GUN_UV_DIM = Vector2i(8, 8)
//val GUN_DIM = Vector2f(1f, 1f)
//
//val SETTINGS_ICON = Vector2i(54, 489)
//val SETTINGS_ICON_UV_DIM = Vector2i(18, 18)
//val SETTINGS_ICON_DIM = Vector2f(4.5f, 4.5f)
//
//
//val WINDOW = Vector2i(26, 496)
//val WINDOW_DIM = Vector2i(9, 9)
//val WINDOW_UV_INSETS = Vector2i(4, 4)
//val WINDOW_INSETS = Vector2f(1f)
//
//
//data class Bullet(val start: Vector2f = Vector2f(), val pos: Vector2f = Vector2f(), val dir: Vector2f = Vector2f(), var tileHit: Float = 0f)
//
//fun Window.mouseWorld(): Vector2f {
//    val mouseX = BufferUtils.createDoubleBuffer(1)
//    val mouseY = BufferUtils.createDoubleBuffer(1)
//    val width = BufferUtils.createIntBuffer(1)
//    val height = BufferUtils.createIntBuffer(1)
//    glfwGetCursorPos(window, mouseX, mouseY)
//    glfwGetWindowSize(window, width, height)
//    return Vector2f(
//        (mouseX.get().toFloat() - 640f) / (1280 / 80f) + camera.x,
//        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f) + camera.y
//    )
//}
//
//fun Window.mouseUi(): Vector2f {
//    val mouseX = BufferUtils.createDoubleBuffer(1)
//    val mouseY = BufferUtils.createDoubleBuffer(1)
//    val width = BufferUtils.createIntBuffer(1)
//    val height = BufferUtils.createIntBuffer(1)
//    glfwGetCursorPos(window, mouseX, mouseY)
//    glfwGetWindowSize(window, width, height)
//    return Vector2f(
//        (mouseX.get().toFloat() - 640f) / (1280 / 80f),
//        (height.get().toFloat() - mouseY.get().toFloat() - 360f) / (720f / 45f)
//    )
//}
//
//val SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 8)
//val DISPATCHER = SERVICE.asCoroutineDispatcher()
//
//interface Match {
//    var id: Int
//    val channel: Channel<suspend Write.() -> (Unit)>
////    val players: Container<Player>
////    val bullets: Container<Bullet>
//    val players: ConcurrentHashMap<Int, Player>
//    val bullets: MutableList<Bullet>
//    val queryAnswers: MutableList<String>
//    var query: Boolean
//    var queryPrompt: String
//    var map: TileMap?
//    var mode: Int
//    var lastPlant: Long
//    suspend fun send(block: suspend Write.() -> Unit)
////    override var id = -1
////    override val channel: Channel<suspend Write.() -> (Unit)> = Channel(
////        capacity = Int.MAX_VALUE,
////        onUndeliveredElement = {
////            println("didnt deliver")
////        }
////    )
////    override var map: TileMap? = null
////    override val bullets = BitSet(256)
////    override val bulletStates = Array(256) { Bullet(Vector2f(), Vector2f(), Vector2f(), 0f) }
////    override val query = Query()
////    override var mode = 0
////    override val players = BitSet()
////    override val playerStates: Array<PlayerState> = Array(256) {
////        object : PlayerState {
////            val lerpPos = Vector2f(1.0f)
////            override val channel: Channel<suspend Write.() -> Unit>
////                get() = TODO("Not yet implemented")
////            override var id = -1
////            override var timePos = 0L
////            override val lastPos = Vector2f(1f)
////            override val nextPos = Vector2f(1f)
////            override val pos: Vector2f get() {
////                val t = ((timeMillis - timePos) / LERP_POS_RATE)
////                return lerpPos.set(
////                    lastPos.x + (nextPos.x - lastPos.x) * t,
////                    lastPos.y + (nextPos.y - lastPos.y) * t,
////                )
////            }
////            override var health = 1f
////            override var alive = true
////            override var planting = false
////            override var defusing = false
////            override var terrorist = false
////            override suspend fun send(block: suspend Write.() -> Unit) { }
////        }
////    }
////    override suspend fun send(block: suspend Write.() -> Unit) {
////        channel.trySend(block)
////    }
//}
//
//val ATLAS = atlas(Paths.get("shooter_flipped.png"))
//val FONT_ATLAS = atlas(Paths.get("vt323.png"))
//val ATLASES = arrayOf(ATLAS, FONT_ATLAS)
//
//
//const val OUT_SHOOT = 0
//const val OUT_POS = 1
//const val OUT_CONFIRM_RESPAWN = 2
//const val OUT_RESPOND_QUERY = 3
//const val OUT_CALL_VOTE = 4
//const val OUT_TRY_PLANT = 5
//const val OUT_CANCEL_PLANT = 6
//
//const val IN_JOIN = 0
//const val IN_EXIT = 1
//const val IN_MAP = 2
//const val IN_POS = 3
//const val IN_SHOOT = 4
//const val IN_DIE = 5
//const val IN_TRY_RESPAWN = 6
//const val IN_RESPAWN = 7
//const val IN_QUERY = 8
//const val IN_CANCEL_QUERY = 9
//const val IN_CONFIRM_PLANT = 10
//
//suspend fun client() = window {
//    title = "title"
//    dim = Vector2i(1280, 720)
//    var settings = false
//    val font = font(Paths.get("vt323.fnt"))
//    val textureShader = Shader(Paths.get("texture.vert"), Paths.get("texture.frag"), 3, 1)
//    val rotateTextureShader = Shader(Paths.get("rotate_texture.vert"), Paths.get("rotate_texture.frag"), 3, 2, 1, 1)
//    val fontShader = Shader(Paths.get("font.vert"), Paths.get("font.frag"), 3, 1, 3)
//    val colorShader = Shader(Paths.get("color.vert"), Paths.get("color.frag"), 3, 4)
//    val fovShader = Shader(Paths.get("fov.vert"), Paths.get("fov.frag"), 3)
//    val textureBuffer = ShaderBuffer(textureShader, 200000, drawTextured(ATLAS, false))
//    val rotateTextureBuffer = ShaderBuffer(rotateTextureShader, 200000, drawTexture(atlas))
//    val uiBuffer = ShaderBuffer(textureShader, 64000, drawTexture(atlas))
//    val uiColorBuffer = ShaderBuffer(colorShader, 64000, drawUntextured())
//    val fontBuffer = ShaderBuffer(fontShader, 64000, drawTexture(fontAtlas))
//    val colorBuffer = ShaderBuffer(colorShader, colorShader.quad, drawUntextured())
//    val fovBuffer = ShaderBuffer(fovShader, 68000, drawUntextured())
//    glEnable(GL_DEPTH_TEST)
//    glDepthFunc(GL_LESS)
//    glEnable(GL_STENCIL_TEST)
//    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
//    val group = AsynchronousChannelGroup.withThreadPool(SERVICE)
//    val provider = SocketProvider(9999, group)
//    val address = Address("127.0.0.1", 9999)
//    val match = object : Match {
//        override var id: Int = -1
//        override val channel = Channel<suspend Write.() -> Unit>(UNLIMITED)
//        override val players = ConcurrentHashMap<Int, Player>()
//        override val bullets = Collections.synchronizedList(ArrayList<Bullet>())
//        override val queryAnswers = Collections.synchronizedList(ArrayList<String>())
//        override var query = false
//        override var queryPrompt = ""
//        override var map: TileMap? = null
//        override var mode: Int = -1
//        override var lastPlant: Long = 0L
//
//        override suspend fun send(block: suspend Write.() -> (Unit)) {
//            channel.trySend(block)
//        }
//    }
//    val player = Player()
//    CoroutineScope(DISPATCHER).launch {
//        match.apply match@{
//            val connection = provider.connect(address)
//            launch {
//                try {
//                    connection.read.apply {
//                        id = int()
//                        println("Joined as ${id}")
//                        map = map()
//                        println("gg1")
//                        camera.set(vec2f())
//                        val playerId = id
//                        players[id] = player.apply player@{ this@player.id = playerId }
//                        println("gg2")
//                        move()
//                        launch {
//                            try { channel.consumeEach { connection.write.it() } }
//                            catch(throwable: Throwable) {
//                                if (throwable is ClosedChannelException) throwable.printStackTrace()
//                            } finally {
//                                connection.close()
//                                channel.close()
//                                //clear
//                            }
//                        }
//                        while (isActive && provider.isOpen && !glfwWindowShouldClose(window)) {
//                            when (int()) {
//                                IN_JOIN -> {
//                                    val joinId = int()
//                                    println("${joinId} joined")
//                                    players[joinId] = Player().apply player@{ this@player.id = joinId }
//                                }
//                                IN_EXIT -> players.remove(id)
//                                IN_SHOOT -> {
//                                    val pos = vec2f()
//                                    val dir = vec2f()
//                                    val tileHit = match.map!!.world.tiledRaycast(match.map!!.worldSize, pos, dir, TiledRay()).distance
//                                    bullets.add(Bullet(pos, Vector2f(pos), dir, tileHit))
//                                }
//                                IN_DIE -> {
//                                    val id = int()
//                                    println("Die(${id})")
//                                    players[id]?.alive = false
//                                }
//                                IN_TRY_RESPAWN -> {
//                                    send { int(OUT_CONFIRM_RESPAWN) }
//                                }
//                                IN_RESPAWN -> {
//                                    val respawnId = int()
//                                    val spawn = vec2f()
//                                    players[respawnId]?.alive = true
//                                    if (respawnId == id) {
//                                        camera.set(spawn)
//                                        move()
//                                    } else players[respawnId]?.apply {
//                                        pos.set(spawn)
//                                        lastPos.set(spawn)
//                                        nextPos.set(spawn)
//                                        timePos = timeMillis
//                                    }
//                                }
//                                IN_POS -> {
//                                    val posId = int()
//                                    val inPos = vec2f()
//                                    val inDir = float()
////                                    println("IN_POS(${id}, (${inPos.x}, ${inPos.y}), ${inDir})")
////                                    println("IN_POS(${id})")
//                                    players[posId]?.apply {
//                                        timePos = timeMillis
//                                        lastPos.set(nextPos)
//                                        nextPos.set(inPos)
//                                        lastDir = nextDir
//                                        nextDir = inDir
//                                    }
//                                }
//                                IN_MAP -> map = map()
//                                IN_QUERY -> {
//                                    println("in query")
//                                    val prompt = string()
//                                    val answerCount = int()
//                                    queryAnswers.clear()
//                                    for (i in 0 until answerCount)
//                                        queryAnswers.add(string())
//                                    query = true
//                                    queryPrompt = prompt
//                                }
//                                IN_CANCEL_QUERY -> {
//                                    println("cancelled")
//                                    query = false
//                                }
//                                IN_CONFIRM_PLANT -> {
//
//                                }
//                            }
//                        }
//                    }
//                } catch(throwable: Throwable) {
//                    throwable.printStackTrace()
//                } finally {
//                    connection.close()
//                    channel.close()
//                    //clear
//                }
//            }
//        }
//    }
//    val collider = Collider(Vector2f(), TILE_SIZE)
//    fun collider(id: Int): Collider =
//        collider.apply { pos.set(if (match.id != id) match.players[id]!!.pos else camera) }
//    mouseEvent += press@{ code, action ->
//        if (!match.players.containsKey(match.id) || settings) return@press
//        val playerState = match.players[match.id]!!
//        if (code != GLFW_MOUSE_BUTTON_1 || action != GLFW_PRESS || !playerState.alive || match.map == null) return@press
//        val mouseWorld = mouseWorld()
//        val theta = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
//        val dir = Vector2f(cos(theta), sin(theta))
//        match.apply {
//            val tileHit = map!!.world.tiledRaycast(map!!.worldSize, camera, dir, TiledRay()).distance
//            bullets.add(Bullet(camera, camera.copy().add(dir), dir, tileHit))
//            send {
//                int(OUT_SHOOT)
//                vec2f(dir)
//            }
//        }
//    }
//    keyEvent += press@ { code, action ->
//        if (code != GLFW_KEY_ESCAPE || action != GLFW_PRESS) return@press
//        settings = !settings
//    }
//    keyEvent += press@ { code, action ->
//        if (code != GLFW_KEY_GRAVE_ACCENT || action != GLFW_PRESS) return@press
//        match.send { int(OUT_CALL_VOTE) }
//    }
//    keyEvent += key@{ code, action ->
//        if (code != GLFW_KEY_E) return@key
//        if (action == GLFW_PRESS) {
//            match.send {
//                int(OUT_TRY_PLANT)
//            }
//        } else if (player.planting && action == GLFW_RELEASE) {
//            match.send {
//                int(OUT_CANCEL_PLANT)
//            }
//        }
//    }
//    val keyIds = GLFW_KEY_1..GLFW_KEY_9
//    keyEvent += press@{ code, action ->
//        if (code !in keyIds || action != GLFW_PRESS || !match.query) return@press
//        match.send {
//            int(OUT_RESPOND_QUERY)
//            int(keyIds.indexOf(code))
//        }
//        match.query = false
//    }
//    var fovTriangles: Int
//    val fovResults = Array(4096) { TiledRay() }
//    var gunDir = 0f
//    onFixed(20) { _, _ ->
//        val mouseWorld = mouseWorld()
//        if (match.map != null && match.players.containsKey(match.id) && match.players[match.id]!!.alive && !settings)
//            gunDir = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
//        match.send {
//            int(OUT_POS)
//            vec2f(camera)
//            float(gunDir)
//        }
//    }
//    onFixed(60) { _, elapsed ->
//        if (!match.players.containsKey(match.id) || match.map == null || !match.players[match.id]!!.alive || settings) return@onFixed
//        val cameraTile = camera.round(Vector2f()).int()
//        val nearbyTileColliders = (0 until COLLISION_SIZE * COLLISION_SIZE).mapNotNull {
//            val tile = Vector2i(it % COLLISION_SIZE, it / COLLISION_SIZE).add(cameraTile).sub(COLLISION_RADIUS, COLLISION_RADIUS)
//            if (tile.x < 0 || tile.x >= match.map!!.worldSize || tile.y < 0 || tile.y >= match.map!!.worldSize) null
//            else {
//                val tileIndex = tile.x + tile.y * match.map!!.worldSize
//                match.map!!.colliders[tileIndex]
//            }
//        }
//        val motion = Vector2f(0f, 0f).apply {
//            if (keyState[GLFW_KEY_W]) y += 1
//            if (keyState[GLFW_KEY_S]) y -= 1
//            if (keyState[GLFW_KEY_A]) x -= 1
//            if (keyState[GLFW_KEY_D]) x += 1
//        }.normal().mul(0.135f)
//        collider(match.id).move(motion, nearbyTileColliders)
//        camera.add(collider(match.id).move(motion, nearbyTileColliders))
//        move()
//    }
//    val fontEntity = fontBuffer.aggregator(64000)
//    val uiEntity = uiBuffer.aggregator(64000)
//    val uiColorEntity = uiColorBuffer.aggregator(64000)
////    val pos = Vector2f(0f, 0f)
////    val dim = Vector2f(10f, 1.5f)
////    val rad = Vector2f(dim.x / 2f, dim.y / 2f)
////    val start = Vector2f(pos).sub(rad)
////    val end = Vector2f(pos).add(rad)
//    val shadowFpsSlider = Slider(
//        Vector2f(0f, 0f),
//        Vector2f(10f, 1.5f),
//        5f, 360f, 144f
//    )
//    val entityFpsSlider = Slider(
//        Vector2f(0f, 5f),
//        Vector2f(10f, 1.5f),
//        5f, 360f, 144f
//    )
//    val subtextSize = 0.015f
//
//    onTick { delta, elapsed ->
//        fontEntity.clear()
//        uiEntity.clear()
//        uiColorEntity.clear()
//        val mouse = mouseUi()
//        val shadowLabelPos = Vector2f(shadowFpsSlider.pos).add(0f, shadowFpsSlider.rad.y).add(0f, font.lineHeight(subtextSize) / 2f)
//        val entityLabelPos = Vector2f(entityFpsSlider.pos).add(0f, entityFpsSlider.rad.y).add(0f, font.lineHeight(subtextSize) / 2f)
////        println("m ${mouse.x}, ${mouse.y}")
////        println("p ${pos.x}, ${pos.y}")
//
////        buttonQuads(uiEntity, fontEntity, pos, dim, uiZ = 10)
//        if (match.query) {
//            val maxLength = font.textWidth(match.queryPrompt, size = subtextSize)
//            fontEntity.fontQuads(font, Vector2f(-30f, 0f), match.queryPrompt, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
//            for ((index, answer) in match.queryAnswers.withIndex()) {
//                val answerLabel = "[${index + 1}]"
//                val dim = font.textWidth(answerLabel, size = subtextSize) + font.textWidth(answer, size = subtextSize)
//                val rad = dim / 2f
//                val answerIndexPos = Vector2f(-30f - rad + font.textRadius(answerLabel, size = subtextSize), font.lineHeight(subtextSize) * -1.2f * (index + 1))
//                val answerPos = Vector2f(-30f - rad + font.textWidth(answerLabel, size = subtextSize) + font.textRadius(answer, size = subtextSize), font.lineHeight(subtextSize) * -1.2f * (index + 1))
//                fontEntity.fontQuads(font, answerIndexPos, answerLabel, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
//                fontEntity.fontQuads(font, answerPos, answer, Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
//            }
//            uiEntity.slicedQuads(Vector2f(-30f, -2f), Vector2f(maxLength + 4f, font.lineHeight(subtextSize) * 1.4f * (match.queryAnswers.size + 1)), WINDOW_INSETS, WINDOW, WINDOW_DIM, WINDOW_UV_INSETS, z = 10)
//        }
//        if (settings) {
//            uiEntity.slicedQuads(Vector2f(0f, 0f), Vector2f(20f, 40f), WINDOW_INSETS, WINDOW, WINDOW_DIM, WINDOW_UV_INSETS, z = 10)
//            uiEntity.textureQuad(Vector2f(-8f + 2.25f, 15f), SETTINGS_ICON_DIM, SETTINGS_ICON, SETTINGS_ICON_UV_DIM, z = 11)
//            fontEntity.fontQuads(font, Vector2f(2.25f, 15f), "Settings", Vector3f(0f, 0f, 0f), z = 11)
//            shadowFpsSlider.update(mouse)
//            entityFpsSlider.update(mouse)
//            fontEntity.fontQuads(font, shadowLabelPos, "Shadow FPS", Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
//            fontEntity.fontQuads(font, entityLabelPos, "Entity FPS", Vector3f(0f, 0f, 0f), size = subtextSize, z = 11)
//            sliderQuads(font, uiColorEntity, fontEntity, shadowFpsSlider.pos, shadowFpsSlider.dim, shadowFpsSlider.cursor, 5f, 360f, z = 11)
//            sliderQuads(font, uiColorEntity, fontEntity, entityFpsSlider.pos, entityFpsSlider.dim, entityFpsSlider.cursor, 5f, 360f, z = 11)
////            uiEntity.textureQuad(pos, dim, SETTINGS_ICON, SETTINGS_ICON_UV_SIZE, z = 11)
////            fontEntity.fontQuads(font, pos, "Your mother", Vector3f(1f, 1f, 0f), z = 11)
//
//        }
//    }
////    button(uiEntity, fontEntity)
//    val unclippedEntity = textureBuffer.aggregator(RENDER_SIZE * RENDER_SIZE * textureShader.quad + textureShader.quad + textureShader.quad * 9)
//    onTick { delta, elapsed ->
//        if (match.map == null || !match.players.containsKey(match.id) || settings) return@onTick
//        unclippedEntity.clear()
//        //Local Player
//        unclippedEntity.textureQuad(
//            camera, TILE_SIZE,
//            if (!match.players.containsKey(match.id) || !match.players[match.id]!!.alive) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_DIM,
//            z = 3
//        )
//        //World
//        for (index in 0 until RENDER_SIZE * RENDER_SIZE) {
//            val cameraTile = camera.round(Vector2f()).int()
//            val tile = Vector2i(index % RENDER_SIZE, index / RENDER_SIZE).add(cameraTile).sub(RENDER_RADIUS, RENDER_RADIUS)
//            if (tile.x < 0 || tile.x >= match.map!!.worldSize || tile.y < 0 || tile.y >= match.map!!.worldSize) continue
//            val tileIndex = tile.let { it.x + it.y * match.map!!.worldSize }
//            unclippedEntity.textureQuad(
//                tile.float(),
//                TILE_SIZE,
//                TILES[match.map!!.world[tileIndex].toInt()],
//                TILE_UV_SIZE,
//                z = if (match.map!!.world[tileIndex] in SOLIDS) 3 else 1
//            )
//        }
//    }
//    val gunEntity = rotateTextureBuffer.aggregator(256 * rotateTextureShader.quad)
//    onFixed({ entityFpsSlider.value.toInt() }) { _, _ ->
//        if (match.map == null || !match.players.containsKey(match.id) || settings) return@onFixed
//        gunEntity.clear()
//        val mouseWorld = mouseWorld()
//        val theta = atan2(camera.y - mouseWorld.y, camera.x - mouseWorld.x) + PI.toFloat()
////        println(theta)
//        var flipped = theta >= PI / 2 && theta <= (PI + PI / 2).toFloat()
//        gunEntity.rotateTextureQuad(
//            Vector2f(camera).add(cos(theta) * 1f, sin(theta) * 1f),
//            GUN_DIM,
//            if (flipped) GUN_FLIPPED else GUN, GUN_UV_DIM,
//            if (flipped) theta - toRadians(180.0).toFloat() else theta,
//            z = 10
//        )
//        match.players.forEach { (_, player) ->
//            flipped = player.dir >= PI / 2 && player.dir <= (PI + PI / 2).toFloat()
//            gunEntity.rotateTextureQuad(
//                Vector2f(player.pos).add(cos(player.dir) * 1f, sin(player.dir) * 1f),
//                GUN_DIM,
//                if (flipped) GUN_FLIPPED else GUN, GUN_UV_DIM,
//                if (flipped) player.dir - toRadians(180.0).toFloat() else player.dir,
//                z = 10
//            )
//        }
//    }
//    val clippedEntity = textureBuffer.aggregator(513 * textureShader.quad)
//    onFixed({ entityFpsSlider.value.toInt() }) { delta, elapsed ->
//        if (match.map == null) return@onFixed
//        val speed = (1f/entityFpsSlider.value.toInt()) * 200f
//        val toRemove = ArrayList<Bullet>()
//        match.bullets.forEach { bullet ->
//            val shootRay = raycastBlocking<Unit>(bullet.pos, bullet.dir.mul(speed * 0.1f, Vector2f()), max = speed) { ray ->
//                match.players.values.firstOrNull {
//                    contains(ray, if (it.id == match.id) camera else it.pos, TILE_RADIUS)
//                } != null
//            }
//            if (shootRay.distance != speed || bullet.start.distance(bullet.pos) > bullet.tileHit) {
//                toRemove.add(bullet)
//            }
//        }
//        toRemove.forEach { match.bullets.remove(it) }
//        clippedEntity.clear()
//        //Bullets
//        match.bullets.forEach { bullet ->
//            clippedEntity.textureQuad(
//                bullet.pos,
//                BULLET_DIM,
//                BULLET,
//                BULLET_UV_DIM,
//                z = 4
//            )
//            bullet.pos.add(bullet.dir.x * speed, bullet.dir.y * speed)
//        }
//        //Players
//        match.players.forEach { (_, player) ->
////            println("yo what ${id} (${pos.x}, ${pos.y})")
//            if (player.id == match.id || player.id >= 256) return@forEach
//            clippedEntity.textureQuad(
//                player.pos, TILE_SIZE,
//                if (!player.alive) DEAD_PLAYER else COUNTER_PLAYER, PLAYER_UV_DIM,
//                z = 3
//            )
//        }
//        //Bomb
////        if (planted) {
////            val frame = floor(elapsed % 4f).toInt()
////            clippedEntity.textureQuad(bomb, TILE_SIZE, BOMB_FRAMES[frame], TILE_UV_SIZE, z = 3)
////            clippedEntity.textureQuad(bomb, DEFUSE_MARK_SIZE, DEFUSE_MARK, DEFUSE_MARK_UV_SIZE, z = 3)
////        }
//    }
//    val fogEntity = colorBuffer.aggregator(colorShader.quad)
//    onTick { delta, elapsed ->
//        if (match.map == null) return@onTick
//        fogEntity.clear()
//        fogEntity.colorQuad(
//            Vector2f(match.map!!.worldSize / 2f - 0.5f, match.map!!.worldSize / 2f - 0.5f),
//            Vector2f(match.map!!.worldSize.toFloat(), match.map!!.worldSize.toFloat()),
//            Vector4f(0f, 0f, 0f, 0.25f), z = 2
//        )
//    }
//    val fovEntity = fovBuffer.aggregator(68000)
//    onFixed({ shadowFpsSlider.value.toInt() }) { delta, elapsed ->
////        println("fps: ${1f/delta}")
//        if (match.map == null || !match.players.containsKey(match.id) || !match.players[match.id]!!.alive) return@onFixed
//        val mouseWorld = mouseWorld()
//        val map = match.map!!
//        fovTriangles = 0
//        map.corners.sortBy {
//            if (mouseWorld.x > camera.x) atan2(it.y - camera.y, it.x - camera.x)
//            else atan2(camera.y - it.y, camera.x - it.x)
//        }
//        val mouse = mouseWorld()
//        val first = (atan2(mouse.y - camera.y, mouse.x - camera.x) - toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
//        val last = (atan2(mouse.y - camera.y, mouse.x - camera.x) + toRadians(41.0).toFloat()).let { Vector2f(cos(it), sin(it)) }
//        map.world.tiledRaycast(map.worldSize, camera, first, fovResults[fovTriangles++])
//        map.corners.forEach {
//            val cornerDist = camera.distance(it)
//            val mouseDist = camera.distance(mouseWorld)
//            val normalizedLook =
//                Vector2f((mouseWorld.x - camera.x) / mouseDist, (mouseWorld.y - camera.y) / mouseDist)
//            val normalizedCorner = Vector2f((it.x - camera.x) / cornerDist, (it.y - camera.y) / cornerDist)
//            val dot = normalizedLook.dot(normalizedCorner)
//            if (cornerDist < RENDER_RADIUS + 10f && dot >= 0.75) {
//                val theta = atan2(camera.y - it.y, camera.x - it.x) + PI.toFloat()
//                val middle = Vector2f(cos(theta), sin(theta))
//                map.apply {
//                    world.tiledRaycast(
//                        worldSize,
//                        camera,
//                        Vector2f(cos(theta - 0.00001f), sin(theta - 0.00001f)),
//                        fovResults[fovTriangles++]
//                    )
//                    world.tiledRaycast(worldSize, camera, middle, fovResults[fovTriangles++])
//                    world.tiledRaycast(
//                        worldSize,
//                        camera,
//                        Vector2f(cos(theta + 0.00001f), sin(theta + 0.00001f)),
//                        fovResults[fovTriangles++]
//                    )
//                }
//            }
//        }
//        map.world.tiledRaycast(map.worldSize, camera, last, fovResults[fovTriangles++])
//        fovEntity.clear()
//        (1 until fovTriangles).forEach {
//            val previous = fovResults[it - 1]
//            val current = fovResults[it]
//            fovEntity.fovTriangle(camera, previous.hit, current.hit, z = 2)
//        }
//    }
//    onTick { delta, elapsed ->
//        glClearColor(236f / 255f, 247f / 255f, 252f / 255f, 1f)
//        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
//        if (match.map != null) {
//            fontEntity.draw()
//            uiEntity.draw()
//            uiColorEntity.draw()
//            unclippedEntity.draw()
//            glStencilFunc(GL_ALWAYS, 1, 0xFF)
//            glStencilMask(0xFF)
//            glColorMask(false, false, false, false)
//            fovEntity.draw()
//            glStencilMask(0x00)
//            glStencilFunc(GL_EQUAL, 1, 0xFF)
//            glColorMask(true, true, true, true)
//            clippedEntity.draw()
//            gunEntity.draw()
//            glStencilFunc(GL_NOTEQUAL, 1, 0xFF)
//            fogEntity.draw()
//            glStencilMask(0xFF)
//            glStencilFunc(GL_ALWAYS, 0, 0xFF)
//        }
//        glfwSwapBuffers(window)
//        glfwPollEvents()
//    }
//}