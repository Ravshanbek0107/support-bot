package uz.supportbot.bot

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Suppress("DEPRECATION")
@Component
class SupportBot(
    private val updateHandler: UpdateHandler
) : TelegramLongPollingBot() {

    @Value("\${telegram.bot.token}") lateinit var tokenValue: String
    @Value("\${telegram.bot.username}") lateinit var usernameValue: String

    override fun getBotUsername() = usernameValue

    override fun getBotToken() = tokenValue

    override fun onUpdateReceived(update: Update) {
        println(update)
        println("update keldi")
        updateHandler.handle(update,this)
    }
}