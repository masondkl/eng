package me.mason.server

import com.github.exerosis.mynt.base.Write
import kotlinx.coroutines.channels.Channel
import me.mason.shared.*
import org.joml.Vector2f
import java.util.*

interface ServerPlayer : Player {
    val channel: Channel<suspend Write.() -> (Unit)>
    suspend fun send(block: suspend Write.() -> (Unit))
}