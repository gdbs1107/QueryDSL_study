package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void setUp() {

        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        //when
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
    @DisplayName("JPQL로 쿼리 작성하기")
    public void startJPQL(){
        //given

        /**
         * 문자로 SQL을 작성함
         * -> 오타가 나더라도 실제 오류는 해당 메서드가 실행 될 때 발생함 (Runtime Exception 발생)
         * */
        String qlString = "select m from Member m" +
                " where m.username =: username";

        Member findByJPQL = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findByJPQL.getId()).isEqualTo(1L);
    }


    @Test
    @DisplayName("QMember new로 초기화하기")
    public void startQueryDSL_new(){

        // 같은 테이블을 조인하여 조회하는 경우에는 임의의 변수명으로 조회해야함
        QMember m = new QMember("m");

        /**
         * 1. QType으로 쿼리를 생성하면 컴파일 시점에 에러가 잡힘
         * 2. 파라미터 바인딩을 직접 해줌 -> SQL Injection에 안전
         *  이건 뭔지 잘 모르겠음
         * */
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("QMember 내부 인스턴스로 초기화하기")
    public void startQueryDSL_instance(){
        QMember m = member;

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getId()).isEqualTo(1L);
    }


    @Test
    @DisplayName("QMember static method로 초기화하기: 가장 권장되는 방식")
    public void startQueryDSL_static_method(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getId()).isEqualTo(1L);
    }


    @Test
    @DisplayName("간단한 검색로직")
    public void search(){

        /**
         * SQL 예약어를 포함한 대부분의 명령어가 지원이 된다
         * */
        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }


    @Test
    @DisplayName("간단한 검색로직_AND")
    public void search_And_param(){

        /**
         * ,를 AND로 쓸 수 있다: 훨씬 직관적인
         *
         * 그리고 얘는 NULL을 무시함 -> 동적쿼리 작성시 매우 강력
         * */
        Member result = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                        ,member.age.eq(10)
                )
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }


    @Test
    @DisplayName("다양한 조회 쿼리들")
    public void result_fetch_test(){
        //given
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        // 페이징용 쿼리
        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(member)
                .fetchResults();

        memberQueryResults.getTotal();
        List<Member> results = memberQueryResults.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }


    /**
     * 회원 정렬 순서
     * 1. 나이 내림차순
     * 2. 이름 올림차순
     *
     * null 데이터를 제일 마지막에 위치시킨다
     * */
    @Test
    @DisplayName("정렬")
    public void title(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }


    @Test
    @DisplayName("페이징")
    public void paging(){
        /**
         * 하나 건너뛰고 두개씩 조회한다
         * */
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }


    @Test
    @DisplayName("페이징")
    public void paging2(){
        /**
         * 하나 건너뛰고 두개씩 조회한다
         * fetchResult는 count쿼리가 따로 발생함 -> 필요에 따라 분리해야 할 수 있음
         * */
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getResults().size()).isEqualTo(2);
    }


    @Test
    @DisplayName("집합")
    public void aggregation(){
        // QueryDSL이 제공하는 Tuple타입으로 반환됨
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.avg())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);

        /**
         * 데이터 타입이 다양하기 때문에 튜플로 반환함
         * 실제 실무에서는 DTO에 맞게 반환하는 것이 가능하기 때문에 DTO를 틀로서 가져옴
         * */
    }


    @Test
    @DisplayName("group by")
    public void group(){

        List<Tuple> result = queryFactory
                .select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
    }


    /**
     * 팀 A에 소속된 모든 회원
     * */
    @Test
    @DisplayName("join")
    public void join(){

        /**
         * 기본 SQL과 JPQL에 대한 이해가 더 필요해보인다
         * */
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }


    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조인
     *
     * result:
     * tuple: [Member(id=1, username=member1, age=10), Team(id=1, name=teamA)]
     * tuple: [Member(id=2, username=member2, age=20), Team(id=1, name=teamA)]
     * tuple: [Member(id=3, username=member3, age=30), null]
     * tuple: [Member(id=4, username=member4, age=40), null]
     *
     * 그냥 join의 경우는 뒤에 where로 쓰면 되지만
     * left/right 조인의 경우는 where가 적용되지 않기 때문에 on을 사용해줘야함
     * */
    @Test
    @DisplayName("join on")
    public void join_on(){
        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple: " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;


    /**
     * 조인 쿼리 시, 쿼리를 두 번 날리는 게 아닌 한 방에 날릴 수 있는 방법
     * -> 성능 최적화에서 많이 사용된다
     *
     * 한 방 쿼리에 너무 집착하지는 말자
     * 객관적으로 생각해보자 당신의 서비스는 정말 그렇게 까지 최적화가 필요한가?
     * 그런게 아니라면 너무 집착하지는 말자
     * */
    @Test
    @DisplayName("페치조인을 사용하지 않음_1")
    public void fetch_join_no(){
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }


    @Test
    @DisplayName("페치조인을 사용하지 않음_2")
    public void fetch_join_no2(){
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team,team)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }



    @Test
    @DisplayName("페치조인")
    public void fetch_join_use(){
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team,team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }


    /**
     * 나이가 가장 많은 회원을 조회
     *
     * JPAExpressions를 이용하고 static-import 해주면 일반 쿼리처럼 사용 가능
     *
     * 하지만 From 절의 서브쿼리는 사용 할 수 없음
     * -> 보통 서브쿼리는 조인으로 해결이 가능하기 때문에 조인으로 바꾸는 방법을 고민해보기
     * -> 쿼리를 두 번으로 나누어서 실행하기
     * -> nativeSQL 사용하기
     *
     * 보통 from 안에 서브쿼리를 사용하는 경우는
     * 너무 많고 복잡한 정보를 쿼리를 통해서 가져오려 하기 때문임
     * 이는 좋지 않음. 서비스 로직에서 바꾸던지, 앞단에게 부탁하던지 해야지 모든 로직을 쿼리로 해결하려하면 재사용성이나 완전성에 무결함을 해칠 수 있음
     *
     * 최대한 DB는 데이터를 퍼올리는 정도로만 사용하자
     * */
    @Test
    @DisplayName("서브쿼리")
    public void sub_Query(){

        // 서브쿼리는 식별자가 달라야해서 QMember를 다시 정의해줘야함 뭔지 알지?
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }



    /**
     * 나이가 평균 이상인 회원
     * */
    @Test
    @DisplayName("서브쿼리2")
    public void sub_Query2(){

        // 서브쿼리는 식별자가 달라야해서 QMember를 다시 정의해줘야함 뭔지 알지?
        QMember memberSub = new QMember("memberSub");

        // goe = 크거나 같다
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }


    /**
     * 나이가 평균 이상인 회원
     * */
    @Test
    @DisplayName("서브쿼리 - in")
    public void sub_Query_in(){

        // 서브쿼리는 식별자가 달라야해서 QMember를 다시 정의해줘야함 뭔지 알지?
        QMember memberSub = new QMember("memberSub");

        // goe = 크거나 같다
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20,30,40);
    }


    @Test
    @DisplayName("select_subQuery")
    public void select_subQuery(){

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple: " + tuple);
        }
    }


    @Test
    @DisplayName("case문")
    public void basic_case(){
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s: " + s);
        }
    }


    /**
     * 이런 로직을 SQL로 해결하려 하지 말아라
     * 이건 예제니까 하는거지
     * 웬만하면 애플리케이션 로직에서 해야함
     * */
    @Test
    @DisplayName("complexCase")
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0,20)).then("0살~20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s: " + s);
        }
    }
}
