package uz.supportbot


enum class UserRole{
    USER,
    OPERATOR
}

enum class Language {
    UZ,
    RU,
    EN
}

enum class UserState{
    START,
    LANG,
    PHONE,
    IN_CHAT,
    RATING,
    MAIN,
    WAITING,
    OPERATOR_LANG,
    OP_START_WORK,
    OP_END_WORK,

}



enum class ChatStatus {
    OPEN,
    CLOSED
}

enum class OperatorStatus{
    ONLINE,
    OFFLINE,
    BUSY
}


enum class SenderType {
    USER,
    OPERATOR
}

enum class MessageType {
    TEXT,
    VOICE,
    AUDIO,
    VIDEO,
    PHOTO,
    GIF,
    VIDEO_NOTE,
    LOCATION,
    DOCUMENT,
    CONTACT,
}


enum class ErrorCode(val code:Int){
    OPERATOR_NOT_FOUND(101),
    USER_NOT_FOUND(102),
    USER_STATE_NOT_SUITABLE(103),
    OPERATOR_STATE_NOT_SUITABLE(104)
}

