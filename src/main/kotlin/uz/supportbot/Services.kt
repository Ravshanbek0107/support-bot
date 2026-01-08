package uz.supportbot

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.PhotoSize
import org.telegram.telegrambots.meta.api.objects.Video
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Locale


interface UserService {
    fun createOrget(chatId: Long , username: String?, fullName: String): User
    fun getUser(chatId: Long): User?
    fun updateLanguage(chatId: Long, language: Language)
    fun updatePhoneNumber(chatId: Long, number: String)
}
@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
): UserService {

    override fun createOrget(chatId: Long, username: String?, fullName: String): User {
        val existingUser = userRepository.findByChatId(chatId)

        if (existingUser != null){
            // agar user botni ochirib tashlagan bolsa user yana qaytdi va botda activ userga aylantiriladi
            if(existingUser.deleted){
                existingUser.deleted = false
                return userRepository.save(existingUser)
            }
            return existingUser
        }

        val newUser = User(
            chatId = chatId,
            username = username,
            fullName = fullName,
            state = UserState.START
        )

        return userRepository.save(newUser)
    }

    override fun getUser(chatId: Long): User? {
        return userRepository.findByChatId(chatId)
    }

    override fun updateLanguage(chatId: Long, language: Language) {
        val user = userRepository.findByChatId(chatId) ?: return
        user.language = language
        userRepository.save(user)
    }

    override fun updatePhoneNumber(chatId: Long, number: String) {
        val user = userRepository.findByChatId(chatId) ?: return
        if (number.startsWith("+")) number.substring(1)
        user.phoneNumber = number
        userRepository.save(user)
    }


}

// user stateni olish, set qilish va tekshirish
interface UserStateService {
    fun getUserState(chatId: Long): UserState
    fun setUserState(chatId:Long,state: UserState)
    fun checkUserState(chatId:Long,state: UserState):Boolean
}
@Service
class UserStateServiceImpl(
    private val userRepository: UserRepository,
): UserStateService {


    override fun getUserState(chatId: Long): UserState {
        val user = userRepository.findByChatId(chatId)
        return user?.state?: UserState.START
    }

    override fun setUserState(chatId: Long,state: UserState) {
        userRepository.findByChatId(chatId)?.let {
            it.state= state
            userRepository.save(it)
        }
    }

    override fun checkUserState(chatId: Long, state: UserState): Boolean {
        if (getUserState(chatId) != state) return false
        return true
    }
}


interface OperatorService{
    fun getOperatorLanguages(operatorId: Long): Set<Language>
    fun toggleLanguage(operator: User, language: Language)
    fun getOperator(chatId: Long): User?
    fun updateIsFrist(chatId: Long, bool: Boolean)
    fun isFirst(chatId: Long): Boolean
    fun changeStatus(chatId: Long, status: OperatorStatus)
    fun getLanguages(operatorId: Long): Set<Language>
    fun finishWork(operator: User)

    //for admin

    fun createOperator(request: OperatorCreateRequest): OperatorResponse
    fun deleteOperator(request: OperatorCreateRequest): OperatorResponse

    fun getAllOperatorStats(): AllOperatorStatsResponse
}

@Service
class OperatorServiceImpl(
    private val userRepository: UserRepository,
    private val operatorLanguageRepository: OperatorLanguageRepository,
    private val ratingRepository: RatingRepository
): OperatorService {

    override fun getOperatorLanguages(operatorId: Long): Set<Language> =
            operatorLanguageRepository.findAllByOperatorId(operatorId)
                .map { it.language }
                .toSet()
    @Transactional
    override fun toggleLanguage(operator: User, language: Language) {
        val exists = operatorLanguageRepository.existsByOperatorIdAndLanguage(operator.id!!, language)

        if (exists) {
            operatorLanguageRepository.findByOperatorIdAndLanguage(operator.id!!, language)
                ?.let { operatorLanguageRepository.delete(it) }
        } else {
            operatorLanguageRepository
                .save(
                    OperatorLanguage(
                        operator,
                        language
                    )
                )
        }
    }

    override fun getOperator(chatId: Long): User? {
        return userRepository.findByChatId(chatId)
    }

    @Transactional
    override fun updateIsFrist(chatId: Long, bool: Boolean) {
        val operator = userRepository.findByChatId(chatId) ?: return
        operator.isFirst = bool
        userRepository.save(operator)
    }

    override fun isFirst(chatId: Long): Boolean {
        val operator = userRepository.findByChatId(chatId) ?: return false
        return operator.isFirst
    }

    @Transactional
    override fun changeStatus(chatId: Long, status: OperatorStatus) {
        val operator = userRepository.findByChatId(chatId) ?: return
        operator.operatorStatus = status
        userRepository.save(operator)

        println("changing oper status to ${operator.operatorStatus}")
    }

    override fun getLanguages(operatorId: Long): Set<Language> {
        return operatorLanguageRepository.findAllByOperatorId(operatorId)
            .map { it.language }
            .toSet()
    }

    override fun finishWork(operator: User) {
        if (operator.operatorStatus != OperatorStatus.BUSY){

            operator.state = UserState.OP_END_WORK
            operator.operatorStatus = OperatorStatus.OFFLINE

            userRepository.save(operator)

        }
    }



    //for admin
    @Transactional
    override fun createOperator(request: OperatorCreateRequest): OperatorResponse {
        val user = userRepository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)
            ?: throw UserNotFoundException()

        if(user.state != UserState.MAIN) throw UserStateNotSuitableException()

        user.role = UserRole.OPERATOR
        user.isFirst = true

        userRepository.save(user)

        return OperatorResponse.toResponse(user)
    }

    @Transactional
    override fun deleteOperator(request: OperatorCreateRequest): OperatorResponse {
        val operator = userRepository.findByPhoneNumberAndDeletedFalse(request.phoneNumber)
            ?: throw OperatorNotFoundException()

        if(operator.state != UserState.MAIN) throw OperatorStateNotSuitableException()

        operator.role = UserRole.USER
        operator.operatorStatus = null

        operatorLanguageRepository.deleteByOperatorId(operator.id!!)

        userRepository.save(operator)

        return OperatorResponse.toResponse(operator)
    }

    override fun getAllOperatorStats(): AllOperatorStatsResponse {
        return AllOperatorStatsResponse(
            ratingRepository.findOperatorStats()
        )
    }


}

interface MessageLangService{
    fun getMessage(key: String,lang: Language): String
}
@Service
class MessageLangServiceImpl(
    @Qualifier("messageLangSource") private val messageSource: MessageSource
): MessageLangService {

    override fun getMessage(key: String,lang: Language): String {
        val language = when(lang) {
            Language.UZ -> Locale("uz")
            Language.RU -> Locale("ru")
            Language.EN -> Locale("en")
        }

        return try{
            messageSource.getMessage(key,emptyArray(),language)
        }catch (e: Exception){
            println("Message key not found: $key for lang: $lang")
            key
        }
    }
}


interface ChatService {

    fun userRequestSupport(user: User): Chat?
    fun operatorStartWork(operator: User): Chat?
    fun cancelWaiting(user: User)
    fun closeChatByOperator(operator: User):Chat?
    fun saveRating(user:User,rating:Int): Boolean
    fun getActiveChatByUser(user: User): Chat?
    fun getActiveChatByOperator(operator: User): Chat?
}

@Service
class ChatServiceImpl(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val operatorLanguageRepository: OperatorLanguageRepository,
    private val userStateService: UserStateService,
    private val operatorService: OperatorService,
    private val ratingRepository: RatingRepository
) : ChatService {

    @Transactional
    override fun userRequestSupport(user: User): Chat? {
        user.waitingSince = Date()
        userRepository.save(user)

        userStateService.setUserState(user.chatId, UserState.WAITING)

        val operator = findFreeOperatorForUser(user) ?: return null
        return createChat(user, operator)
    }
    @Transactional
    override fun operatorStartWork(operator: User): Chat? {
        operator.waitingSince = Date()
        userRepository.save(operator)

        operatorService.changeStatus(operator.chatId, OperatorStatus.ONLINE)
        userStateService.setUserState(operator.chatId, UserState.OP_START_WORK)
        val user = findWaitingUserForOperator(operator) ?: return null
        return createChat(user, operator)

    }
    @Transactional
    override fun cancelWaiting(user: User) {
        user.waitingSince = null
        userRepository.save(user)
        userStateService.setUserState(user.chatId, UserState.MAIN)
    }

    @Transactional
    override fun closeChatByOperator(operator: User):Chat? {
        val chat = chatRepository.findByOperatorChatIdAndStatus(operator.chatId, ChatStatus.OPEN) ?: return null

        chat.status = ChatStatus.CLOSED
        chat.closedAt = Date()
        chatRepository.save(chat)

        operator.operatorStatus = OperatorStatus.ONLINE
        userRepository.save(operator)

        val user = chat.user
        user.ratingChatId = chat.id
        userRepository.save(user)

        userStateService.setUserState(chat.user.chatId, UserState.RATING)
        userStateService.setUserState(operator.chatId, UserState.OP_START_WORK)

        return chat
    }

    @Transactional
    override fun saveRating(user: User, rating: Int): Boolean {

        val chatId = user.ratingChatId ?: return false
        val chat = chatRepository.findById(chatId).orElse(null) ?: return false

        val closedAt = chat.closedAt ?: return false

        //agar 24 soatdan oshib ketsa rating qabul qilinmaydi
        val hours = Duration.between(
            closedAt.toInstant(),
            Instant.now()
        ).toHours()

        if (hours > 24) {
            user.ratingChatId = null
            userRepository.save(user)
            return false
        }

        ratingRepository.save(
            Rating(
                chat = chat,
                operator = chat.operator,
                user = user,
                rating = rating
            )
        )

        user.ratingChatId = null
        userRepository.save(user)

        return true
    }

    @Transactional(readOnly = true)
    override fun getActiveChatByUser(user: User): Chat? {
        val chat = chatRepository.findByUserChatIdAndStatus(user.chatId, ChatStatus.OPEN)
        chat?.operator?.chatId
        return chat
    }

    @Transactional(readOnly = true)
    override fun getActiveChatByOperator(operator: User): Chat? {
        val chat = chatRepository.findByOperatorChatIdAndStatus(operator.chatId, ChatStatus.OPEN)
        chat?.user?.chatId
        return chat
    }


    private fun createChat(user: User, operator: User):Chat {

        operator.operatorStatus = OperatorStatus.BUSY
        operator.waitingSince = null
        userRepository.save(operator)

        user.waitingSince = null
        userRepository.save(user)

        val chat = Chat(
            user = user,
            operator = operator,
            status = ChatStatus.OPEN,
            chatLang = user.language!!,
            openedAt = Date()
        )
        chatRepository.save(chat)

        userStateService.setUserState(user.chatId, UserState.IN_CHAT)
        userStateService.setUserState(operator.chatId, UserState.IN_CHAT)
        return chat
    }


    private fun findFreeOperatorForUser(user: User): User? {
        return userRepository.findFreeOperatorByLang(user.language!!.name)
    }

    private fun findWaitingUserForOperator(operator: User): User? {
        return userRepository.findWaitingUserByLangs(operator.id!!)
    }
}


interface ChatMessageService {

    fun validateAndSaveMessage(message: Message, chat: Chat, sender: User): MessageValidationResult

}
@Service
class ChatMessageServiceImpl(
    private val messageRepository: MessageRepository
): ChatMessageService {

    val MAX_PHOTO_SIZE = 5 * 1024 * 1024L
    val MAX_VIDEO_SIZE = 20 * 1024 * 1024L

    override fun validateAndSaveMessage(message: Message, chat: Chat, sender: User): MessageValidationResult {


        if (message.hasPhoto()) {
            val photo = message.photo.maxByOrNull { it.fileSize }!!

            //rasm hajmini tek qilish
            if (photo.fileSize > MAX_PHOTO_SIZE) {
                return MessageValidationResult(false, "error.file.too.large")
            }

            savePhotoMessage(message, chat, sender, photo)
            return MessageValidationResult(true)
        }


        if (message.hasVideo()) {
            val video = message.video

            //video hajmini tek qilish
            if (video.fileSize > MAX_VIDEO_SIZE) {
                return MessageValidationResult(false, "error.file.too.large")
            }

            saveVideoMessage(message, chat, sender, video)
            return MessageValidationResult(true)
        }

        // Text
        if (message.hasText()) {
            saveTextMessage(message, chat, sender)
            return MessageValidationResult(true)
        }

        // bizda yoq bolgan xabar turi
        saveOtherMessage(message, chat, sender)
        return MessageValidationResult(true)
    }

    private fun saveTextMessage(message: Message, chat: Chat, sender: User) {
        messageRepository.save(
            Message(
                chat = chat,
                sender = sender,
                senderType = if (sender.role == UserRole.USER) SenderType.USER else SenderType.OPERATOR,
                messageType = MessageType.TEXT,
                messageId = message.messageId.toLong(),
                text = message.text
            )
        )
    }

    private fun savePhotoMessage(message: Message, chat: Chat, sender: User, photo: PhotoSize) {
        messageRepository.save(
            Message(
                chat = chat,
                sender = sender,
                senderType = if (sender.role == UserRole.USER) SenderType.USER else SenderType.OPERATOR,
                messageType = MessageType.PHOTO,
                messageId = message.messageId.toLong(),
                caption = message.caption,
                fileId = photo.fileId,
                fileUniqueId = photo.fileUniqueId,
                fileSize = photo.fileSize.toLong()
            )
        )
    }

    private fun saveVideoMessage(message: Message, chat: Chat, sender: User, video: Video) {
        messageRepository.save(
            Message(
                chat = chat,
                sender = sender,
                senderType = if (sender.role == UserRole.USER) SenderType.USER else SenderType.OPERATOR,
                messageType = MessageType.VIDEO,
                messageId = message.messageId.toLong(),
                caption = message.caption,
                fileId = video.fileId,
                fileUniqueId = video.fileUniqueId,
                fileSize = video.fileSize.toLong()
            )
        )
    }

    private fun saveOtherMessage(message: Message, chat: Chat, sender: User) {
        val messageType = when {
            message.hasVoice() -> MessageType.VOICE
            message.hasAudio() -> MessageType.AUDIO
            message.hasDocument() -> MessageType.DOCUMENT
            message.hasVideoNote() -> MessageType.VIDEO_NOTE
            message.hasLocation() -> MessageType.LOCATION
            message.hasContact() -> MessageType.CONTACT
            message.hasAnimation() -> MessageType.GIF
            else -> MessageType.TEXT
        }

        val fileId = when {
            message.hasVoice() -> message.voice.fileId
            message.hasAudio() -> message.audio.fileId
            message.hasDocument() -> message.document.fileId
            message.hasVideoNote() -> message.videoNote.fileId
            message.hasAnimation() -> message.animation.fileId
            else -> null
        }

        messageRepository.save(
            Message(
                chat = chat,
                sender = sender,
                senderType = if (sender.role == UserRole.USER) SenderType.USER else SenderType.OPERATOR,
                messageType = messageType,
                messageId = message.messageId.toLong(),
                caption = message.caption,
                fileId = fileId,
                latitude = if (message.hasLocation()) message.location.latitude else null,
                longitude = if (message.hasLocation()) message.location.longitude else null
            )
        )
    }
}


interface EditMessageService {
    fun updateMessage(message: Message?)
}
@Service
class EditMessageServiceImpl(
    private val editMessageRepository: EditMessageRepository
): EditMessageService {
    override fun updateMessage(message: Message?) {
        TODO("Not yet implemented")
    }


}

interface CallbackQueryService {
    fun updateMessage(message: CallbackQuery)
}
@Service
class CallbackQueryServiceImpl(

): CallbackQueryService {
    override fun updateMessage(message: CallbackQuery) {
        TODO("Not yet implemented")
    }

}





