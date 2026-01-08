package uz.supportbot

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.util.Date

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @LastModifiedBy var lastModifiedBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false,
)


@Entity
@Table(name = "users")
class User(

    @Column(nullable = false, unique = true, length = 12) var chatId: Long,

    @Column(length = 50) var username: String? = null,

    @Column(nullable = false) var fullName: String,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING) var operatorStatus: OperatorStatus? = null,

    @Enumerated(EnumType.STRING) var language: Language? = null,

    @Column(length = 20, unique = true) var phoneNumber: String? = null,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var state: UserState,

    @Column(nullable = false) var isFirst: Boolean = false,

    @Temporal(TemporalType.TIMESTAMP) var waitingSince: Date? = null,

    var ratingChatId: Long? = null

): BaseEntity()


@Entity
@Table(name = "admins") // security qoshilgani uchun alohida table qildim
class Admin(

    @Column(nullable = false) var fullName: String,

    @Column(nullable = false, unique = true) var username: String,

    @Column(nullable = false) var password: String,

): BaseEntity()

@Entity
@Table(name = "operator_languages")// bu holatda biz multilang qilishimiz mumkin operatorni
class OperatorLanguage(

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id", nullable = false) var operator: User,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var language: Language

): BaseEntity()


@Entity
@Table(name = "chats")
class Chat(

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) var user: User,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id", nullable = false) var operator: User,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var status: ChatStatus = ChatStatus.OPEN,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var chatLang: Language,

    @Temporal(TemporalType.TIMESTAMP) var openedAt: Date? = null,

    @Temporal(TemporalType.TIMESTAMP) var closedAt: Date? = null

): BaseEntity()


@Entity
@Table(name = "messages")
class Message(

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_id", nullable = false) var chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_id", nullable = false) var sender: User,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var senderType: SenderType,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var messageType: MessageType,

    @Column(nullable = false) var messageId: Long,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reply_to_id") var replyTo: Message? = null,

    @Column(columnDefinition = "TEXT") var text: String? = null,

    @Column(columnDefinition = "TEXT") var caption: String? = null,

    var fileId: String? = null,
    var fileUniqueId: String? = null,
    var fileSize: Long? = null,

    var forwardFromChatId: Long? = null,
    var forwardFromMessageId: Long? = null,


    var latitude: Double? = null,
    var longitude: Double? = null,

    @Column(nullable = false) var edited: Boolean = false,
): BaseEntity()

@Entity
@Table(name = "edit_messages")
class EditMessage(

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "message_id", nullable = false) var message: Message,

    @Enumerated(EnumType.STRING) @Column(nullable = false) var messageType: MessageType,

    @Column(columnDefinition = "TEXT") var newText: String? = null,
    @Column(columnDefinition = "TEXT") var newCaption: String? = null,

    var newFileId: String? = null,
    var newFileUniqueId: String? = null,
    var newFileSize: Long? = null,


): BaseEntity()


@Entity
@Table(name = "ratings")
class Rating(

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chat_id", nullable = false) var chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operator_id", nullable = false) var operator: User,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) var user: User,

    var rating: Int? = null,

):BaseEntity()


