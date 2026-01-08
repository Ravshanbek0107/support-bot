package uz.supportbot

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.AsyncHandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.support.RequestContextUtils
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import uz.supportbot.bot.SupportBot
import java.util.Locale

@Configuration
class BotConfig {

    @Bean
    fun registerBot(bot: SupportBot): Boolean {
        val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
        return try {
            botsApi.registerBot(bot)
            println("Bot registered successfully")
            true
        } catch (e: TelegramApiException) {
            e.printStackTrace()
            false
        }
    }
}

@Configuration
class WebMvcConfig : WebMvcConfigurer {

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("error")
    }

    @Bean
    fun messageLangSource(): MessageSource {
        return ResourceBundleMessageSource().apply {
            setDefaultEncoding(Charsets.UTF_8.name())
            setBasenames("messages")
        }
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : AsyncHandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

                request.getHeader("hl")?.let {
                    RequestContextUtils.getLocaleResolver(request)
                        ?.setLocale(request, response, Locale(it))
                }
                return true
            }
        })
    }
}
