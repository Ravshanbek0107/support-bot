package uz.supportbot

import jakarta.persistence.EntityManager
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
}


class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted  = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}


@Repository
interface UserRepository : BaseRepository<User> {
    fun findByChatId(chatId: Long): User?
    @Query("""
    select u.*
    from users u
    where u.state = 'WAITING'
      and u.deleted = false
      and u.language in (
          select ol.language
          from operator_languages ol
          where ol.operator_id = :operatorId
      )
    order by u.waiting_since asc
    limit 1
    """, nativeQuery = true)
    fun findWaitingUserByLangs(@Param("operatorId") operatorId: Long): User?

    @Query("""
    select u.*
    from users u
    where u.role = 'OPERATOR'
      and u.operator_status = 'ONLINE'
      and u.deleted = false
      and exists (
          select 1
          from operator_languages ol
          where ol.operator_id = u.id
            and ol.language = :lang
      )
    order by u.waiting_since asc
    limit 1
    """,
        nativeQuery = true)
    fun findFreeOperatorByLang(@Param("lang") lang: String): User?


    fun findByPhoneNumberAndDeletedFalse(phone:String): User?

}

@Repository
interface MessageRepository : BaseRepository<Message>{}

@Repository
interface EditMessageRepository : BaseRepository<EditMessage>{}

@Repository
interface AdminRepository : BaseRepository<Admin>{}

@Repository
interface ChatRepository : BaseRepository<Chat>{
    fun findByUserChatIdAndStatus(chatId: Long, status: ChatStatus): Chat?

    fun findByOperatorChatIdAndStatus(chatId: Long, status: ChatStatus): Chat?
}

@Repository
interface OperatorLanguageRepository : BaseRepository<OperatorLanguage>{

    fun findAllByOperatorId(operatorId: Long): List<OperatorLanguage>

    fun existsByOperatorIdAndLanguage(operatorId: Long, language: Language): Boolean

    fun findByOperatorIdAndLanguage(operatorId: Long, language: Language): OperatorLanguage?

    fun deleteByOperatorId(id:Long)

}

@Repository
interface RatingRepository : BaseRepository<Rating>{

    @Query("""
        select new uz.supportbot.OperatorStatResponse(
            o.id,
            o.username, 
            o.fullName,
            o.phoneNumber,
            avg(r.rating),
            count(r.id)
        )
        from User o
        left join Rating r on r.operator.id = o.id
        where o.role = 'OPERATOR'
        group by o.id, o.username, o.fullName, o.phoneNumber
    """)
    fun findOperatorStats(): List<OperatorStatResponse>

}

























