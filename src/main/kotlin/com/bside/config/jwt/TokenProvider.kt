package com.bside.config.jwt

import com.bside.dto.TokenDto
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import java.util.stream.Collectors

/**
 * name : TokenProvider
 * author : jisun.noh
 */
@Component
class TokenProvider {

    private val log = LoggerFactory.getLogger(TokenProvider::class.java)

    private val AUTHORITIES_KEY = "auth"
    private val BEARER_TYPE = "bearer"
    private val ACCESS_TOKEM_EXPIRE_TIME = 1000 * 60 * 10 // 30 min
    private val REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60 * 24 * 7 // 7day

    //Secret 값은 특정 문자열을 Base64 로 인코딩한 값 사용
    private val secretKey: String = "YnNpZGUtcHJvamVjdC1iYWNrZW5kLXByb2plY3QtMTEtMnRlYW0tZmluYWwtZmlnaHRpbmc"
    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

    fun generateTokenDto(authentication: Authentication): TokenDto {
        //권한들 가져오기
        val authorities = authentication.authorities.stream()
            .map { obj: GrantedAuthority -> obj.authority }
            .collect(Collectors.joining(","))

        val now = Date().time

        //Access Token 생성
        val accessTokenExpiredsIn = Date(now + ACCESS_TOKEM_EXPIRE_TIME)
        val accessToken = Jwts.builder()
            .setSubject(authentication.name) //payload "sub":"name"
            .claim(AUTHORITIES_KEY, authorities) //payload "auth":"ROLE_USER"
            .setExpiration(accessTokenExpiredsIn)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        //Refresh Token 생성
        val refreshToken = Jwts.builder()
            .setExpiration(Date(now + REFRESH_TOKEN_EXPIRE_TIME))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

        return TokenDto().apply {
            this.grantType = BEARER_TYPE
            this.accessToken = accessToken
            this.accessTokenExpiresIn = accessTokenExpiredsIn.time
            this.refreshToken = refreshToken
        }
    }

    /**
     * description : 토큰 복호화
     */
    fun getAuthentication(accessToken: String): Authentication {
        // 토큰 복호화
        val claims: Claims = parseClaims(accessToken)

        if (claims.get(AUTHORITIES_KEY) == null) {
            throw RuntimeException("권한 정보가 없는 토큰입니다.")
        }

        // 클레임에서 권한 정보 가져오기
        val authorities: Collection<GrantedAuthority?> =
            Arrays.stream(claims[AUTHORITIES_KEY].toString().split(",").toTypedArray())
                .map { role: String? -> SimpleGrantedAuthority(role) }
                .collect(Collectors.toList())

        val principal: UserDetails = User(claims.subject, "", authorities)

        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            return true
        } catch (e: SecurityException) {
            log.info("잘못된 JWT 서명입니다.")
        } catch (e: ExpiredJwtException) {
            log.info("만료된 JWT 토큰입니다.")
        } catch (e: UnsupportedJwtException) {
            log.info("지원되지 않는 JWT 토큰입니다.")
        } catch (e: IllegalArgumentException) {
            log.info("JWT 토큰이 잘못되었습니다.")
        }
        return false
    }

    private fun parseClaims(accessToken: String): Claims {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).body
        } catch (e: ExpiredJwtException) {
            return e.claims
        }
    }

}