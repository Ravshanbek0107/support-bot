package uz.supportbot.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.bots.AbsSender
import uz.supportbot.ChatMessageService
import uz.supportbot.ChatService
import uz.supportbot.Language
import uz.supportbot.MessageLangService
import uz.supportbot.OperatorService
import uz.supportbot.User
import uz.supportbot.UserRole
import uz.supportbot.UserService
import uz.supportbot.UserState
import uz.supportbot.UserStateService


@Component
class UpdateHandler (
    private val userMessageHandler: UserMessageHandler,
    private val operatorMessageHandler: OperatorMessageHandler,
    private val editMessageHandler: EditMessageHandler,
    private val callbackQueryHandler: CallbackQueryHandler,
    private val userService: UserService
){
    fun handle(update: Update,bot: AbsSender) {
        when{
            update.hasMessage() -> handleMessage(update,bot)
            update.hasEditedMessage() -> editMessageHandler.handle(update.editedMessage,bot)
            update.hasCallbackQuery() -> callbackQueryHandler.handle(update.callbackQuery,bot)
        }
    }

    private fun handleMessage(update: Update,bot: AbsSender) {
        val chatId = update.message.chatId

        val user = userService.getUser(chatId)

        when (user?.role) {
            UserRole.USER -> userMessageHandler.handle(update.message,bot)
            UserRole.OPERATOR -> operatorMessageHandler.handle(update.message,bot)
            null -> userMessageHandler.handle(update.message,bot)
        }
    }
}


@Component
class UserMessageHandler (
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val sendMessageService: SendMessageService,
    private val replyKeyboardFactory: ReplyKeyboardFactory,
    private val inlineKeyboardFactory: InlineKeyboardFactory,
    private val messageLangService: MessageLangService,
    private val chatService: ChatService,
    private val chatMessageService: ChatMessageService
){

    fun handle(message: Message,bot: AbsSender) {
        val chatId = message.chatId
        val text = message.text
        val username = message.from.userName
        val fullname = listOfNotNull(message.from.firstName, message.from.lastName).joinToString(" ")

        val user = userService.createOrget(chatId,username,fullname)

        if (text == "/start") {
            handleStart(user, bot)
            return
        }


        when (user.state) {
            UserState.PHONE -> handlePhone(message, user,bot)
            UserState.MAIN -> handleMainButton(message, user,bot)
            UserState.WAITING -> {
                if (message.text == messageLangService.getMessage("button.cancel.waiting", user.language!!)) {
                    chatService.cancelWaiting(user)
//                    userStateService.setUserState(user.chatId, UserState.MAIN)

                    sendMessageService.sendMessage(
                        user.chatId,
                        messageLangService.getMessage("message.main.menu", user.language!!),
                        replyKeyboardFactory.userMainMenu(user.language!!),
                        bot
                    )
                    return
                }
            }
            UserState.IN_CHAT -> handleChatMessage(message, user, bot)

            else -> {}
        }

    }

    private fun handleStart(user:User,bot: AbsSender) {
        val chatId = user.chatId

        if (user.state == UserState.RATING || user.state == UserState.IN_CHAT || user.state == UserState.WAITING)return

        if (user.state == UserState.MAIN) {
            sendMessageService.sendText(
                chatId,
                messageLangService.getMessage("message.start", user.language!!)+user.fullName,
                bot
            )
            showUserMain(chatId, user,bot)
            return
        }

        userStateService.setUserState(chatId, UserState.LANG)
//        sendMessageService.sendMessage(
//            user.chatId,
//            ".",
//            ReplyKeyboardRemove(true),
//            bot
//        )
        sendMessageService.sendMessage(
            chatId,
            "Tilni tanlang: \nВыберите язык: \nChoose language:",
            inlineKeyboardFactory.langButton(),
            bot
        )
    }

    private fun handlePhone(message: Message, user: User,bot: AbsSender) {
        val chatId = message.chatId

        if (!message.hasContact()) {
            sendMessageService.sendMessage(
                chatId,
                messageLangService.getMessage(
                    "message.contact.request",
                    user.language!!
                ),
                replyKeyboardFactory.contactRequest(user.language!!),
                bot
            )
            return
        }

        val contact = message.contact

        if (contact.userId == null || contact.userId != message.from.id) {
            sendMessageService.sendMessage(
                chatId,
                messageLangService.getMessage(
                    "error.send.own.contact",
                    user.language!!
                ),
                replyKeyboardFactory.contactRequest(user.language!!),
                bot
            )
            return
        }

        userService.updatePhoneNumber(chatId, contact.phoneNumber)
        userStateService.setUserState(chatId, UserState.MAIN)

        showUserMain(chatId, user,bot)
    }

    private fun handleMainButton(message: Message, user: User, bot: AbsSender) {
        val text = message.text ?: return

        when (text) {
            messageLangService.getMessage("button.main.change.lang", user.language!!) -> {
                userStateService.setUserState(user.chatId, UserState.LANG)

//                sendMessageService.sendMessage(
//                    user.chatId,
//                    ".",
//                    ReplyKeyboardRemove(true),
//                    bot
//                )

                sendMessageService.sendMessage(
                    user.chatId,
                    messageLangService.getMessage("message.choose.lang", user.language!!),
                    inlineKeyboardFactory.langButton(),
                    bot
                )
            }

            messageLangService.getMessage("button.main.support", user.language!!) -> {
                val chat = chatService.userRequestSupport(user)

                if (chat != null) {
                    sendMessageService.sendMessage(
                        user.chatId,
                        messageLangService.getMessage("message.chat.started", user.language!!)
                            +chat.operator.fullName,
                        ReplyKeyboardRemove(true),
                        bot
                    )

                    sendMessageService.sendMessage(
                        chat.operator.chatId,
                        messageLangService.getMessage("message.chat.started.operator", chat.operator.language!!)
                            +chat.user.fullName,
                        replyKeyboardFactory.closeChat(chat.operator.language!!),
                        bot
                    )
                } else {
                    sendMessageService.sendMessage(
                        user.chatId,
                        messageLangService.getMessage("message.waiting.operator", user.language!!),
                        replyKeyboardFactory.cancelWaiting(user.language!!),
                        bot
                    )
                }
            }

            else -> return
        }
    }

    private fun handleChatMessage(message: Message, user: User, bot: AbsSender) {
        val chat = chatService.getActiveChatByUser(user) ?: run {
            userStateService.setUserState(user.chatId, UserState.MAIN)
            return
        }

        val validationResult = chatMessageService.validateAndSaveMessage(message, chat, user)

        if (!validationResult.isValid) {
            sendMessageService.sendText(
                user.chatId,
                messageLangService.getMessage(validationResult.errorKey!!, user.language!!),
                bot
            )
            return
        }
        // operatorga axbar yuborish
        sendToOperator(chat.operator.chatId, message, bot)
    }

    private fun sendToOperator(operatorChatId: Long, message: Message, bot: AbsSender) {
        when {
            message.hasText() -> {
                sendMessageService.sendText(operatorChatId, message.text, bot)
            }
            message.hasPhoto() -> {
                val photo = message.photo.maxByOrNull { it.fileSize }!!
                sendMessageService.sendPhoto(operatorChatId, photo.fileId, message.caption, bot)
            }
            message.hasVideo() -> {
                sendMessageService.sendVideo(operatorChatId, message.video.fileId, message.caption, bot)
            }
            message.hasVoice() -> {
                sendMessageService.sendVoice(operatorChatId, message.voice.fileId, message.caption, bot)
            }
            message.hasAudio() -> {
                sendMessageService.sendAudio(operatorChatId, message.audio.fileId, message.caption, bot)
            }
            message.hasDocument() -> {
                sendMessageService.sendDocument(operatorChatId, message.document.fileId, message.caption, bot)
            }
            message.hasVideoNote() -> {
                sendMessageService.sendVideoNote(operatorChatId, message.videoNote.fileId, bot)
            }
            message.hasLocation() -> {
                sendMessageService.sendLocation(operatorChatId, message.location.latitude, message.location.longitude, bot)
            }
            message.hasContact() -> {
                sendMessageService.sendContact(
                    operatorChatId,
                    message.contact.phoneNumber,
                    message.contact.firstName,
                    message.contact.lastName,
                    bot
                )
            }
            message.hasAnimation() -> {
                sendMessageService.sendAnimation(operatorChatId, message.animation.fileId, message.caption, bot)
            }
        }
    }

    private fun showUserMain(chatId: Long, user: User,bot: AbsSender) {
        sendMessageService.sendMessage(
            chatId,
            messageLangService.getMessage("message.main.menu", user.language!!),
            replyKeyboardFactory.userMainMenu(user.language!!),
            bot
        )
    }
}


@Component
class OperatorMessageHandler (
    private val operatorService: OperatorService,
    private val userStateService: UserStateService,
    private val sendMessageService: SendMessageService,
    private val replyKeyboardFactory: ReplyKeyboardFactory,
    private val messageLangService: MessageLangService,
    private val inlineKeyboardFactory: InlineKeyboardFactory,
    private val chatService: ChatService,
    private val chatMessageService: ChatMessageService,
){

fun handle(message: Message,bot: AbsSender) {
        val chatId = message.chatId
        val text = message.text

        val operator = operatorService.getOperator(chatId) ?: return

        if (text == "/start") {
            handleStart(operator, bot)
            return
        }

        when (operator.state) {

            UserState.MAIN -> handleMainInput(message, operator, bot)

            UserState.OP_START_WORK -> {
                if (message.text == messageLangService.getMessage("button.finish.work", operator.language!!)) {
                    operatorService.finishWork(operator)
                    userStateService.setUserState(operator.chatId, UserState.MAIN)

                    sendMessageService.sendMessage(
                        operator.chatId,
                        messageLangService.getMessage("message.main.menu", operator.language!!),
                        replyKeyboardFactory.operatorMainMenu(operator.language!!),
                        bot
                    )
                    return
                }

            }

            UserState.IN_CHAT -> {
                if(message.text == messageLangService.getMessage("button.close.chat", operator.language!!)){
                        val chat = chatService.closeChatByOperator(operator)!! // bu yerda chat doim boladi chunki chat bolmasa close button chiqmaydi

                        sendMessageService.sendMessage(
                            operator.chatId,
                            messageLangService.getMessage("message.chat.closed", operator.language!!)
                                +chat.user.fullName,
                            replyKeyboardFactory.finishWork(operator.language!!),
                            bot
                        )
                        operatorStartWork(operator,bot)// bu avtomatik yana userga boglanish uchun agar bor bolsa

                        sendMessageService.sendMessage(
                            chat.user.chatId,
                            messageLangService.getMessage("message.chat.closed.rating", operator.language!!)
                                +chat.operator.fullName,
                            inlineKeyboardFactory.ratingChat(),
                            bot
                        )
                }else {
                    handleChatMessage(message, operator, bot)
                }
            }
            else -> return
        }
    }

    private fun handleChatMessage(message: Message, operator: User, bot: AbsSender) {

        val chat = chatService.getActiveChatByOperator(operator) ?: run {
            userStateService.setUserState(operator.chatId, UserState.MAIN)
            return
        }

        val validationResult = chatMessageService.validateAndSaveMessage(message, chat, operator)

        if (!validationResult.isValid) {
            sendMessageService.sendText(
                operator.chatId,
                messageLangService.getMessage(validationResult.errorKey!!, operator.language!!),
                bot
            )
            return
        }

        sendToUser(chat.user.chatId, message, bot)
    }

    private fun sendToUser(userChatId: Long, message: Message, bot: AbsSender) {
        when {
            message.hasText() -> {
                sendMessageService.sendText(userChatId, message.text, bot)
            }
            message.hasPhoto() -> {
                val photo = message.photo.maxByOrNull { it.fileSize }!!
                sendMessageService.sendPhoto(userChatId, photo.fileId, message.caption, bot)
            }
            message.hasVideo() -> {
                sendMessageService.sendVideo(userChatId, message.video.fileId, message.caption, bot)
            }
            message.hasVoice() -> {
                sendMessageService.sendVoice(userChatId, message.voice.fileId, message.caption, bot)
            }
            message.hasAudio() -> {
                sendMessageService.sendAudio(userChatId, message.audio.fileId, message.caption, bot)
            }
            message.hasDocument() -> {
                sendMessageService.sendDocument(userChatId, message.document.fileId, message.caption, bot)
            }
            message.hasVideoNote() -> {
                sendMessageService.sendVideoNote(userChatId, message.videoNote.fileId, bot)
            }
            message.hasLocation() -> {
                sendMessageService.sendLocation(userChatId, message.location.latitude, message.location.longitude, bot)
            }
            message.hasContact() -> {
                sendMessageService.sendContact(
                    userChatId,
                    message.contact.phoneNumber,
                    message.contact.firstName,
                    message.contact.lastName,
                    bot
                )
            }
            message.hasAnimation() -> {
                sendMessageService.sendAnimation(userChatId, message.animation.fileId, message.caption, bot)
            }
        }
    }




private fun handleStart(operator: User, bot: AbsSender) {

        if (operator.state == UserState.IN_CHAT || operator.state == UserState.OP_START_WORK) {
            return
        }

        if (operatorService.isFirst(operator.chatId)) {
            userStateService.setUserState(operator.chatId, UserState.OPERATOR_LANG)

//            sendMessageService.sendMessage(
//                operator.chatId,
//                ".",
//                ReplyKeyboardRemove(true),
//                bot
//            )

            val selectedLangs = operatorService.getLanguages(operator.chatId)

            sendMessageService.sendMessage(
                operator.chatId,
                messageLangService.getMessage("message.lang.change",operator.language!!),
                inlineKeyboardFactory.languageSelection(selectedLangs,operator),
                bot
            )
            return
        }

        showMainMenu(operator, bot)
    }

    private fun handleMainInput(message: Message,operator:User, bot: AbsSender) {
        if (message.text == null) return

        when (message.text) {

            messageLangService.getMessage("button.main.start.work",operator.language!!) -> {
                operatorStartWork(operator,bot)

            }

            messageLangService.getMessage("button.main.change.lang", operator.language!!) -> {
                userStateService.setUserState(operator.chatId, UserState.OPERATOR_LANG)

//                sendMessageService.sendMessage(
//                    operator.chatId,
//                    ".",
//                    ReplyKeyboardRemove(true),
//                    bot
//                )

                val selectedLangs = operatorService.getLanguages(operator.chatId)

                sendMessageService.sendMessage(
                    operator.chatId,
                    messageLangService.getMessage("message.lang.change",operator.language!!),
                    inlineKeyboardFactory.languageSelection(selectedLangs,operator),
                    bot
                )
            }


            else -> return
        }
    }

    private fun operatorStartWork(operator: User, bot: AbsSender) {
        val chat = chatService.operatorStartWork(operator)

        if (chat != null) {
            sendMessageService.sendMessage(
                operator.chatId,
                messageLangService.getMessage("message.chat.started.operator", operator.language!!)
                        +chat.user.fullName,
                replyKeyboardFactory.closeChat(operator.language!!),
                bot
            )

            sendMessageService.sendMessage(
                chat.user.chatId,
                messageLangService.getMessage("message.chat.started", chat.user.language!!)
                        +chat.operator.fullName,
                ReplyKeyboardRemove(true),
                bot
            )
        } else {
            sendMessageService.sendMessage(
                operator.chatId,
                messageLangService.getMessage("message.start.work", operator.language!!),
                replyKeyboardFactory.finishWork(operator.language!!),
                bot
            )
        }
    }


    private fun showMainMenu(operator: User, bot: AbsSender) {
        userStateService.setUserState(operator.chatId, UserState.MAIN)

        sendMessageService.sendMessage(
            operator.chatId,
            messageLangService.getMessage("message.main.menu", operator.language!!),
            replyKeyboardFactory.operatorMainMenu(operator.language!!),
            bot
        )
    }

}


@Component
class EditMessageHandler (){

    fun handle(message: Message,bot: AbsSender) {}

}




@Component
class CallbackQueryHandler (
    private val userService: UserService,
    private val userStateService: UserStateService,
    private val sendMessageService: SendMessageService,
    private val replyKeyboardFactory: ReplyKeyboardFactory,
    private val messageLangService: MessageLangService,
    private val inlineKeyboardFactory: InlineKeyboardFactory,
    private val operatorService: OperatorService,
    private val chatService: ChatService
){

    fun handle(callbackQuery: CallbackQuery, bot: AbsSender) {
            val chatId = callbackQuery.message.chatId
            val data = callbackQuery.data

            val user = userService.getUser(chatId)

            if(data.startsWith("LANG_")){

                if (user?.role != UserRole.USER) return
                if (user.state != UserState.LANG) return

                handleLanguage(chatId,callbackQuery,bot)
                return
            }

            if (data.startsWith("OP_LANG_TOGGLE")){

                if (user?.role != UserRole.OPERATOR) return
                if (user.state != UserState.OPERATOR_LANG) return

                handleToggleLanguage(chatId,callbackQuery,bot)
                return
            }

            if(data.startsWith("RATING_")){

                if (user?.role != UserRole.USER) return
                if (user.state != UserState.RATING) return

                handleRating(chatId,data,bot)
                return
            }

            if (data.startsWith("OP_LANG_CONFIRM")){

                if (user?.role != UserRole.OPERATOR) return
                if (user.state != UserState.OPERATOR_LANG) return

                handleLangConfirm(callbackQuery,user,bot)
                return
            }
    }

    private fun handleLanguage(chatId:Long,callbackQuery: CallbackQuery,bot: AbsSender) {
            val language = when (callbackQuery.data.removePrefix("LANG_")) {
                "UZ" -> Language.UZ
                "RU" -> Language.RU
                "EN" -> Language.EN
                else -> return
            }

            sendMessageService.deleteMessage(
                chatId,
                callbackQuery.message.messageId,
                bot
            )

            userService.updateLanguage(chatId, language)

            val user = userService.getUser(chatId)
            if(user?.phoneNumber != null ){
                userStateService.setUserState(chatId, UserState.MAIN)
                sendMessageService.sendMessage(
                    chatId,
                    messageLangService.getMessage("message.main.menu", language),
                    replyKeyboardFactory.userMainMenu(language),
                    bot
                )
                return
            }

            userStateService.setUserState(chatId, UserState.PHONE)

            sendMessageService.sendMessage(
                chatId,
                messageLangService.getMessage("message.contact.request", language),
                replyKeyboardFactory.contactRequest(language),
                bot
            )
    }
    private fun handleToggleLanguage(chatId: Long, callbackQuery: CallbackQuery,bot: AbsSender) {
            val lang = Language.valueOf(callbackQuery.data.removePrefix("OP_LANG_TOGGLE:"))
            val operator = operatorService.getOperator(chatId)
            operatorService.toggleLanguage(operator!!, lang)

            val selectedLangs = operatorService.getLanguages(operator.chatId)

            sendMessageService.editMessageReplyMarkup(
                chatId,
                callbackQuery.message.messageId,
                inlineKeyboardFactory.languageSelection(selectedLangs,operator),
                bot
            )
    }

    private fun handleLangConfirm(callbackQuery: CallbackQuery, operator: User,bot: AbsSender) {
        val chatId = callbackQuery.message.chatId
        sendMessageService.deleteMessage(
            chatId,
            callbackQuery.message.messageId,
            bot
        )
        operatorService.updateIsFrist(chatId,false)
        userStateService.setUserState(chatId, UserState.MAIN)
        sendMessageService.sendMessage(
            chatId,
            messageLangService.getMessage("message.main.menu", operator.language!!),
            replyKeyboardFactory.operatorMainMenu(operator.language!!),
            bot
        )

    }

    private fun handleRating(chatId: Long, data: String,bot: AbsSender) {
        val rating = data.removePrefix("RATING_").toInt()
        val user = userService.getUser(chatId) ?: return

        val saved = chatService.saveRating(user, rating)

        if (!saved) {
            sendMessageService.sendMessage(
                chatId,
                messageLangService.getMessage("message.rating.timeout",user.language!!),
                replyKeyboardFactory.userMainMenu(user.language!!),
                bot
            )
            userStateService.setUserState(chatId, UserState.MAIN)
        }else{
            sendMessageService.sendMessage(
                chatId,
                messageLangService.getMessage("message.rating.accepted",user.language!!),
                replyKeyboardFactory.userMainMenu(user.language!!),
                bot
            )

            userStateService.setUserState(chatId, UserState.MAIN)
        }
    }

}


