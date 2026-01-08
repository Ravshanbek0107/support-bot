package uz.supportbot.bot

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import uz.supportbot.Language
import uz.supportbot.MessageLangService
import uz.supportbot.OperatorService
import uz.supportbot.User

@Component
class InlineKeyboardFactory(
    private val operatorService: OperatorService,
    private val messageLangService: MessageLangService
){
    fun ratingChat() = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("1 ⭐\uFE0F").apply{ callbackData ="RATING_1" },
                InlineKeyboardButton("2 ⭐\uFE0F").apply{ callbackData ="RATING_2" },
                InlineKeyboardButton("3 ⭐\uFE0F").apply{ callbackData ="RATING_3" },
                InlineKeyboardButton("4 ⭐\uFE0F").apply{ callbackData ="RATING_4" },
                InlineKeyboardButton("5 ⭐\uFE0F").apply{ callbackData ="RATING_5" }
            )
        )
    )
    fun langButton() = InlineKeyboardMarkup(
        listOf(
            listOf(
                InlineKeyboardButton("\uD83C\uDDFA\uD83C\uDDFF O`zbek").apply{ callbackData ="LANG_UZ" },
                InlineKeyboardButton("\uD83C\uDDF7\uD83C\uDDFA Русский").apply{ callbackData ="LANG_RU" },
                InlineKeyboardButton("\uD83C\uDDEC\uD83C\uDDE7 English").apply{ callbackData ="LANG_EN" },

            )
        )
    )


    fun languageSelection(selectedLangs: Set<Language>, operator: User): InlineKeyboardMarkup {

        fun btn(lang: Language): InlineKeyboardButton {
            val selected = selectedLangs.contains(lang)
            return InlineKeyboardButton(
                (if (selected) "✅ " else "❌ ") + lang.name
            ).apply {
                callbackData = "OP_LANG_TOGGLE:${lang.name}"
            }
        }

        return InlineKeyboardMarkup(
            listOf(
                listOf(btn(Language.UZ)),
                listOf(btn(Language.RU)),
                listOf(btn(Language.EN)),
                listOf(
                    InlineKeyboardButton(messageLangService.getMessage("button.lang.confirm",operator.language!!)).apply {
                        callbackData = "OP_LANG_CONFIRM"
                    }
                )
            )
        )
    }




}


@Component
class ReplyKeyboardFactory(
    private val messageLangService: MessageLangService
){

    fun contactRequest(lang: Language): ReplyKeyboardMarkup {
        val btnText = messageLangService.getMessage("button.share.contact", lang)
        return ReplyKeyboardMarkup().apply {
            keyboard = listOf(
                KeyboardRow(mutableListOf(KeyboardButton(btnText).apply { requestContact = true }))
            )
            resizeKeyboard = true
            oneTimeKeyboard = true
        }
    }

    fun userMainMenu(lang: Language): ReplyKeyboardMarkup {
        val btnSupport = messageLangService.getMessage("button.main.support", lang)
        val btnChangeLang = messageLangService.getMessage("button.main.change.lang", lang)
        return ReplyKeyboardMarkup().apply {
            keyboard = listOf(
                KeyboardRow(mutableListOf(KeyboardButton(btnSupport))),
                KeyboardRow(mutableListOf(KeyboardButton(btnChangeLang)))
            )
            resizeKeyboard = true
            oneTimeKeyboard = true
        }
    }

    fun operatorMainMenu(lang: Language): ReplyKeyboardMarkup {
        val row = KeyboardRow().apply {
            add(messageLangService.getMessage("button.main.start.work",lang))
            add(messageLangService.getMessage("button.main.change.lang",lang))
        }

        return ReplyKeyboardMarkup().apply {
            keyboard = listOf(row)
            resizeKeyboard = true
            oneTimeKeyboard = false
        }
    }
    fun cancelWaiting(lang: Language): ReplyKeyboardMarkup {
            val row = KeyboardRow().apply {
                add(messageLangService.getMessage("button.cancel.waiting",lang))
            }
            return ReplyKeyboardMarkup().apply {
                keyboard = listOf(row)
                resizeKeyboard = true
                oneTimeKeyboard = false
            }
    }

    fun closeChat(lang: Language): ReplyKeyboardMarkup {
            val row = KeyboardRow().apply {
                add(messageLangService.getMessage("button.close.chat",lang))
            }
            return ReplyKeyboardMarkup().apply {
                keyboard = listOf(row)
                resizeKeyboard = true
                oneTimeKeyboard = false
            }
    }
    fun finishWork(lang: Language): ReplyKeyboardMarkup {
            val row = KeyboardRow().apply {
                add(messageLangService.getMessage("button.finish.work",lang))
            }
            return ReplyKeyboardMarkup().apply {
                keyboard = listOf(row)
                resizeKeyboard = true
                oneTimeKeyboard = false
            }
    }



}