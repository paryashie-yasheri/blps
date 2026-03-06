package soaringLizards.blps.repository

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import soaringLizards.blps.jooq.generated.tables.references.USERS
import soaringLizards.blps.model.dto.PageRequest
import soaringLizards.blps.model.dto.PageResult
import soaringLizards.blps.model.user.User
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class UserRepository(
    private val dsl: DSLContext,
) {

    fun list(page: PageRequest): PageResult<User> {
        val items = dsl.selectFrom(USERS)
//            .orderBy(USERS.CREATED_AT.desc())
            .limit(page.limit)
            .offset(page.offset)
            .fetchInto(User::class.java)
        val total = dsl.fetchCount(USERS).toLong()
        return PageResult(items, total)
    }

    fun findById(id: UUID): User? =
        dsl.selectFrom(USERS)
            .where(USERS.ID.eq(id))
            .fetchOneInto(User::class.java)

    fun findByEmail(email: String): User? =
        dsl.selectFrom(USERS)
            .where(DSL.lower(USERS.EMAIL).eq(email.lowercase()))
            .fetchOneInto(User::class.java)

    fun existsById(id: UUID): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(USERS)
                .where(USERS.ID.eq(id))
        )

    fun existsByIdOrEmail(id: UUID, email: String): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(USERS)
                .where(USERS.ID.eq(id).or(DSL.lower(USERS.EMAIL).eq(email.lowercase())))
        )

    fun insert(id: UUID, email: String, role: String, phone: String?, passwordHash: String) {
        dsl.insertInto(USERS)
            .set(USERS.ID, id)
            .set(USERS.EMAIL, email)
            .set(USERS.ROLE, role)
//            .set(USERS.PHONE, phone)
            .set(USERS.PASSWORD_HASH, passwordHash)
            .execute()
    }

    fun updateRole(id: UUID, role: String, updatedAt: OffsetDateTime = OffsetDateTime.now()): Int =
        dsl.update(USERS)
            .set(USERS.ROLE, role)
//            .set(USERS.UPDATE_AT, updatedAt)
            .where(USERS.ID.eq(id))
            .execute()

    fun existsAdminExcept(userId: UUID): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(USERS)
                .where(USERS.ROLE.eq("admin").and(USERS.ID.ne(userId)))
        )

    fun deleteById(id: UUID): Int =
        dsl.deleteFrom(USERS)
            .where(USERS.ID.eq(id))
            .execute()
}