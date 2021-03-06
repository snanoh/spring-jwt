package com.bside.service

import com.bside.entity.Member
import com.bside.repository.MemberReposiroty
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

/**
 * name : CustomUserDetailsService
 * author : jisun.noh
 */
@Service
@Transactional
class CustomUserDetailsService(
    val memberRepository: MemberReposiroty
): UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails? {
        val member = memberRepository.findByEmail(username)
        if (member != null) {
            return createUserDetails(member)
        } else {
            throw UsernameNotFoundException("$username -> 데이터베이스에서 찾을 수 없습니다.")
        }
    }

    // DB 에 User 값이 존재한다면 UserDetails 객체로 만들어서 리턴
    private fun createUserDetails(member: Member): UserDetails? {
        val grantedAuthority: GrantedAuthority = SimpleGrantedAuthority(member.authority.toString())
        return User(
            java.lang.String.valueOf(member.id),
            member.password,
            Collections.singleton(grantedAuthority)
        )
    }
}