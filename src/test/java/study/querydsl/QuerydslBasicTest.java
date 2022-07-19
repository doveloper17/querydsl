package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void beforeEach() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJQPL() throws Exception {
        //given
        String memberName = "member1";

        //when
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", memberName)
                .getSingleResult();

        //then
        assertEquals(memberName, findMember.getUsername());
    }

    @Test
    public void startQuerydsl() throws Exception {
        //given
        /**
         * QMember 사용 방법
         *
         * QMember m = new QMember("m") (같은 테이블 조인하는 경우 사용)
         *
         * QMember.member
         * =
         * import static study.querydsl.entity.QMember.*; 하여 'member'로 사용 (권장)
         */
        String memberName = "member1";

        //when
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        assertEquals(memberName, findMember.getUsername());
    }

    @Test
    public void search() throws Exception {
        //given
        String memberName = "member1";
        int memberAge = 10;

        //when
        /**
         * Querydsl은 JPQL이 제공하는 검색 조건을 제공한다.
         *
         * .eq(~): =
         * .ne(~): !=
         * .eq(~).not(): !=
         *
         * .isNotNull(): is not null
         *
         * .in(~,~): in (~,~)
         * .notIn(~,~): not int (~,~)
         * .between(~,~): between ~,~
         *
         * .goe(~): >=
         * .gt(~): >
         * .loe(~): <=
         * .lt(~): <
         *
         * .like(~%): like ~%
         * .contains(~): like %~%
         * .startsWith(~): like ~%
         */
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        //then
        assertEquals(memberName, findMember.getUsername());
        assertEquals(memberAge, findMember.getAge());
    }

    @Test
    public void searchAndParam() throws Exception {
        //given
        String memberName = "member1";
        int memberAge = 10;

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        //then
        assertEquals(memberName, findMember.getUsername());
        assertEquals(memberAge, findMember.getAge());
    }
}
