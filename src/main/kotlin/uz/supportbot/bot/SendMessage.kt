package uz.supportbot.bot

import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation
import org.telegram.telegrambots.meta.api.methods.send.SendAudio
import org.telegram.telegrambots.meta.api.methods.send.SendContact
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendLocation
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendVideo
import org.telegram.telegrambots.meta.api.methods.send.SendVideoNote
import org.telegram.telegrambots.meta.api.methods.send.SendVoice
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.bots.AbsSender
import uz.supportbot.Language
import uz.supportbot.MessageLangService
import uz.supportbot.MessageType


interface SendMessageService {
    fun sendMessage(chatId: Long, text: String, keyboard: ReplyKeyboard? = null, bot: AbsSender)

    fun sendMessage(
        chatId: Long,
        messageKey: String,
        language: Language,
        keyboard: ReplyKeyboard? = null,
        bot: AbsSender
    )

    fun editMessageReplyMarkup(
        chatId: Long,
        messageId: Int,
        keyboard: ReplyKeyboard? = null,
        bot: AbsSender
    )
//    fun editMessageText(
//        chatId: Long,
//        messageId: Int,
    //    text: String,
//        bot: AbsSender
//    )
    fun sendText(
        chatId: Long,
        text: String,
        bot: AbsSender
    )
    fun sendPhoto(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )
    fun sendVideo(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )
    fun sendVoice(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )

    fun sendAudio(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )

    fun sendDocument(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )
    fun sendVideoNote(
        chatId: Long,
        fileId: String,
        bot: AbsSender
    )
    fun sendLocation(
        chatId: Long,
        latitude: Double,
        longitude: Double,
        bot: AbsSender
    )

    fun sendContact(
        chatId: Long,
        phoneNumber: String,
        firstName: String,
        lastName: String? = null,
        bot: AbsSender
    )

    fun sendAnimation(
        chatId: Long,
        fileId: String,
        caption: String? = null,
        bot: AbsSender
    )
    fun deleteMessage(
        chatId: Long,
        messageId: Int,
        bot: AbsSender
    )
}
@Service
class SendMessageServiceImpl(
    private val messageLangService: MessageLangService
): SendMessageService {

    override fun sendMessage(chatId: Long, text: String, keyboard: ReplyKeyboard?, bot: AbsSender) {
        val message = SendMessage(chatId.toString(), text).apply {
            keyboard?.let { replyMarkup = it }
        }
        bot.execute(message)
    }

    override fun sendMessage(chatId: Long, messageKey: String, language: Language, keyboard: ReplyKeyboard?, bot: AbsSender) {
        val text = messageLangService.getMessage(messageKey, language)

        sendMessage(chatId, text, keyboard,bot)
    }

    override fun editMessageReplyMarkup(
        chatId: Long,
        messageId: Int,
        keyboard: ReplyKeyboard?,
        bot: AbsSender
    ) {
        val edit = EditMessageReplyMarkup().apply {
            this.chatId = chatId.toString()
            this.messageId = messageId
            keyboard?.let { replyMarkup = it as InlineKeyboardMarkup? }
        }

        bot.execute(edit)
    }

    override fun sendText(
        chatId: Long,
        text: String,
        bot: AbsSender
    ) {
        val message = SendMessage(chatId.toString(), text)
        bot.execute(message)
    }

    override fun sendPhoto(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val photo = SendPhoto().apply {
                this.chatId = chatId.toString()
                this.photo = InputFile(fileId)
                caption?.let { this.caption = it
            }
        }
        bot.execute(photo)
    }

    override fun sendVideo(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val video = SendVideo().apply {
                this.chatId = chatId.toString()
                this.video = InputFile(fileId)
                caption?.let { this.caption = it
            }
        }
        bot.execute(video)
    }

    override fun sendVoice(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val voice = SendVoice().apply {
            this.chatId = chatId.toString()
            this.voice = InputFile(fileId)
            caption?.let { this.caption = it }
        }
        bot.execute(voice)
    }

    override fun sendAudio(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val audio = SendAudio().apply {
            this.chatId = chatId.toString()
            this.audio = InputFile(fileId)
            caption?.let { this.caption = it }
        }
        bot.execute(audio)
    }

    override fun sendDocument(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val document = SendDocument().apply {
            this.chatId = chatId.toString()
            this.document = InputFile(fileId)
            caption?.let { this.caption = it }
        }
        bot.execute(document)
    }

    override fun sendVideoNote(chatId: Long, fileId: String, bot: AbsSender) {
        val videoNote = SendVideoNote().apply {
            this.chatId = chatId.toString()
            this.videoNote = InputFile(fileId)
        }
        bot.execute(videoNote)
    }

    override fun sendLocation(chatId: Long, latitude: Double, longitude: Double, bot: AbsSender) {
        val location = SendLocation().apply {
            this.chatId = chatId.toString()
            this.latitude = latitude
            this.longitude = longitude
        }
        bot.execute(location)
    }

    override fun sendContact(
        chatId: Long,
        phoneNumber: String,
        firstName: String,
        lastName: String?,
        bot: AbsSender
    ) {
        val contact = SendContact().apply {
            this.chatId = chatId.toString()
            this.phoneNumber = phoneNumber
            this.firstName = firstName
            lastName?.let { this.lastName = it }
        }
        bot.execute(contact)
    }

    override fun sendAnimation(chatId: Long, fileId: String, caption: String?, bot: AbsSender) {
        val animation = SendAnimation().apply {
            this.chatId = chatId.toString()
            this.animation = InputFile(fileId)
            caption?.let { this.caption = it }
        }
        bot.execute(animation)
    }

    override fun deleteMessage(
        chatId: Long,
        messageId: Int,
        bot: AbsSender
    ) {
        val delete = DeleteMessage(chatId.toString(), messageId)
        bot.execute(delete)
    }


}
