import net.gridmi.api.notifier.app.Client
import net.gridmi.api.notifier.app.Notification
import net.gridmi.api.notifier.app.Server
import net.gridmi.api.notifier.app.Subscriber
import net.gridmi.api.notifier.app.utils.Waiter
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

object TestNotifier {

    private const val HOST = "localhost"
    private const val PORT = 5000

    @Test
    fun exchangeBetweenTwoClientsWithSimpleNotification() {

        Server(port = PORT).start()

        val result = Waiter<Notification>()

        val clientOne = Client(HOST, PORT, Subscriber.Who(
            id = UUID.randomUUID().toString(),
            topics = listOf("topicOne")
        )).apply {
            onNotification = object : Client.Listener.OnNotification {
                override fun onNotification(notification: Notification) {
                    result.data = notification
                }
            }
            start()
        }

        val clientTwo = Client(HOST, PORT, Subscriber.Who(
            id = UUID.randomUUID().toString(),
            topics = listOf("topicTwo")
        )).apply {
            start()
        }

        val uuid: String = UUID.randomUUID().toString()

        clientTwo.sendNotification(
            Notification.joinWithSeparator(
            Notification.include(Notification.TOPIC, "topicOne")
        ), uuid)

        result.await().let { notification ->
            assertEquals(uuid, notification.data)
            assertEquals(clientTwo.who.id, notification.from)
            assert(clientOne.who.isCompletable(notification))
        }

    }

}