package me.mason.client.component

import com.github.exerosis.mynt.base.Address
import com.github.exerosis.mynt.base.Provider
import com.github.exerosis.mynt.base.Read
import com.github.exerosis.mynt.base.Write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.nio.channels.ClosedChannelException

suspend fun Networking(address: Address, provider: Provider) {
    CoroutineScope(Dispatchers.IO).launch {
        val connection = provider.connect(address)
        val channel = Channel<suspend Write.() -> (Unit)>(UNLIMITED)
        //in the future do a handshake or something
        val id = connection.read.int()

        launch {
            try { channel.consumeEach { connection.write.it() } }
            catch(throwable: Throwable) {
                if (throwable is ClosedChannelException) {
                    println("Closing connection")
                }
            } finally {
                connection.close()
                channel.close()
            }
        }

        launch {

        }
    }
}