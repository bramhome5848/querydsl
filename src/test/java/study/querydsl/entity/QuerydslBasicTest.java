package study.querydsl.entity;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach //각 테스트 전에 실행
    public void before() {

        //초기화
        queryFactory = new JPAQueryFactory(em);

        //given
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

    @Test   //member1 찾기
    public void startJPAL() {

        //when
        String qlString =
                "select m from Member m " +
                " where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test   //member1 찾기
    public void startQuerydsl() {   //컴파일 시점에 오류를 잡을 수 있음

        //when
        //QMember m = new QMember("m");    //별칭 직접 지정 (엔티티의 별칭) -> 같은 테이블을 조인할 경우 각각 다른 alias 필요
        //QMember m = QMember.member;      //기본 인스턴스 사용 (엔티티의 별칭)

        /**
         * 실제 querydsl은 jpql로 변형되서 실행됨
         * static import 사용
         * static 변수로 선언되어 있기 때문에 member 사용시 새로운 객체가 반환되는 것이 아님
         * static 선언의 경우 메모리 할당을 딱 한번만 하게 되기 때문에 같은 곳의 메모리 주소만을 바라보게 됨
         * 따라서 static 변수의 값을 공유하게 되는 것
         */

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))    //파라미터 바인딩
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void serach() {

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {

        //when
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {

        //when
        //List
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        //단 건
        Member findMember1 = queryFactory
                .selectFrom(member)
                .fetchOne();

        //처음 한 건 조회
        Member findMember2 = queryFactory
                .selectFrom(member)
                .fetchFirst();

        //페이징에서 사용 -> query 2번 실행
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        //results.getTotal();
        //results.getResults();

        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
    }
}
