package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    @PersistenceUnit
    EntityManagerFactory emf;

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

    @Test
    public void resultFetch() throws Exception {
        //given
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        /**
         * .fetchOne(): 단일 결과가 아니면 error 발생
         * .fetchFirst(): 단일 결과가 아니더라도 limit 1을 하기 때문에 에러가 발생하지 않음.
         */
        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchFirst();

        /**
         * 
         * .fetchCount(), .fetchResults() 는 deprecatede 되었다.
         *
         * groupby, having 절을 사용하는 등의 복잡한쿼리에서는 잘 작동하지 않는다.
         * https://velog.io/@nestour95/QueryDsl-fetchResults%EA%B0%80-deprecated-%EB%90%9C-%EC%9D%B4%EC%9C%A0
         */
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        long total = results.getTotal();
        List<Member> content = results.getResults();
    }

    @Test
    public void sort() throws Exception {
        //given
        /**
         * 회원 정렬 순서
         * 1. 회원 나이 내림차순(desc)
         * 2. 회원 이름 올림차순(asc)
         * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
         */
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertEquals("member5", member5.getUsername());
        assertEquals("member6", member6.getUsername());
        assertNull(memberNull.getUsername());

    }

    @Test
    public void paging1() throws Exception {
        //given //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        //then
        assertEquals(2, result.size());
    }

    @Test
    public void paging2() throws Exception {
        //given //when
        Long count = queryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        //then
        assertEquals(4, count);
    }

    @Test
    public void aggregation() throws Exception {
        //given //when
        Tuple result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();

        //then
        assertEquals(4, result.get(member.count()));
        assertEquals(100, result.get(member.age.sum()));
        assertEquals(25, result.get(member.age.avg()));
        assertEquals(40, result.get(member.age.max()));
        assertEquals(10, result.get(member.age.min()));
    }

    @Test
    public void group() throws Exception {
        //given
        //팀의 이름과 각 팀의 평균 연령을 구해라.

        // when
        List<Tuple> results = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();


        //then
        Tuple teamA = results.get(0);
        Tuple teamB = results.get(1);

        assertEquals(15, teamA.get(member.age.avg()));
        assertEquals(35, teamB.get(member.age.avg()));
        assertEquals("teamA", teamA.get(team.name));
        assertEquals("teamB", teamB.get(team.name));
    }


    @Test
    public void join() throws Exception {
        //given
        //팀 A에 소속된 모든 회원

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     *
     * 외부 조인 불가능 -> on을 사용하면 외부 조인 가능
     */
    @Test
    public void theta_join() throws Exception {
        //given
        //회원의 이름이 팀 이름과 같은 회원 조회
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * inner join의 on은 where에서 필터링 하는 것과 기능이 동일하다.
     *
     * 내부조인(inner join): where에서 필터링
     * 외부조인(left join): on에서 필터링
     */
    @Test
    public void join_on_filtering() throws Exception {
        //given
        /**
         * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
         * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
         */

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    /**
     * 연관관계 없는 엔티티 외부 조인
     */
    @Test
    public void join_on_no_relation() throws Exception {
        //given
        //회원의 이름이 팀 이름과 같은 대상 외부 조인
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) // 막 조인
//                .leftJoin(member.team, team).on(member.username.eq(team.name)) //member.team.id = team.id
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void no_fetch_join() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertFalse(loaded, "페치 조인 미적용");
    }

    @Test
    public void fetch_join() throws Exception {
        //given
        em.flush();
        em.clear();

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertTrue(loaded, "페치 조인 적용");
    }

    @Test
    public void subQueryEq() throws Exception {
        //given
        //나이가 가장 많은 회원 조회

        //when
        QMember m = new QMember("m");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(m.age.max())
                                .from(m)
                ))
                .fetch();

        //then
        assertEquals(40, result.get(0).getAge());

    }

    @Test
    public void subQueryGoe() throws Exception {
        //given
        //나이가 평균 이상인 회원

        //when
        QMember m = new QMember("m");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(m.age.avg())
                                .from(m)
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);

    }

    @Test
    public void subQueryIn() throws Exception {
        //given
        //나이가 20, 30, 40살인 회원

        //when
        QMember m = new QMember("m");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(m.age)
                                .from(m)
                                .where(m.age.gt(10))
                ))
                .fetch();

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(20, 30, 40);

    }

    /**
     * JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     * 당연히 Querydsl 또한 지원하지 않는다.
     *
     * 해결방안
     * 1. 서브쿼리를 join으로 변경한다.(불가능한 상황도 존재한다.)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     * 3. nativeSQL을 사용한다.
     *
     * from 절에서 서브쿼리르 사용하는 경우
     *  - 화면에 맞춰서 쿼리를 작성하게 되는 경우(요즘 추세에 좋은 방법은 아님)
     */
    @Test
    public void selectSubQuery() throws Exception {
        QMember m = new QMember("m");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(m.age.avg())
                                .from(m)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void basicCase() throws Exception {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 강사님은 이러한 문제들은 db에서 해결하지 않는다고 한다.
     * 애플리케이션 또는 화면단에서 처리하여 해결한다.(권장)
     */
    @Test
    public void complexCase() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.age,
                        member.username,
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0 ~ 20살")
                                .when(member.age.between(21, 30)).then("21 ~ 30살")
                                .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void constant() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * .stringValue() 다른 타입을 string으로 변환하는데 사용한다.
     * 특히 Enum 처리할때 필요하다.
     */
    @Test
    public void concat() throws Exception {

        //{username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.like("member%"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() throws Exception {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void tupleProjection() {
        /**
         * Tuple 객체가 service 단으로 넘어가지 않는 것이 좋은 설계임.
         */
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * setter로 값 대입
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,     //MemberDto 기본생성자가 필요하다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 바로 대입
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,     //MemberDto 기본생성자가 필요하다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 생성자로 대입
     */
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,     //MemberDto 기본생성자가 필요하다.
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Projections.bean(), Projections.filed()
     * 속성 이름이 같지 않으면 해당 속성에는 null이 들어간다.
     * 따라서 alias가 필요하다.
     *
     */
    @Test
    public void findUserDto() {
        QMember m = new QMember("m");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), //ExpressionUtils.as(member.username, "name"), 같은 기능
                        ExpressionUtils.as(
                                select(m.age.max())
                                    .from(m), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * Projections.constructor: 런타임 에러
     * @QueryProjection: 컴파일 에러,
     *      - Q 파일을 생성해야함.
     *      - Dto가 Querydsl 라이브러리에 의존적이게 된다.(Dto는 여러 레이어에 존재하기 때문에 좋은 구조는 아니다.)
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertEquals(1, result.size());
    }

    /**
     * 조립을 할 수 있다는게 가장 큰 장점이다.
     */
    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) //.where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }
}
