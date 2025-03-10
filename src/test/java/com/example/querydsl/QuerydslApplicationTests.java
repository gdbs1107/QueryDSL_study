package com.example.querydsl;

import com.example.querydsl.entity.Hello;
import com.example.querydsl.entity.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
// @Transactional이 있으면 자동으로 rollback 시킨다
@Transactional
@Commit
class QuerydslApplicationTests {

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("QHello 엔티티 동작 확인")
    public void qHello_successful(){
        //given
        Hello hello = new Hello();
        entityManager.persist(hello);

        //when
        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        QHello qHello = QHello.hello;

        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        //then
        Assertions.assertThat(result).isEqualTo(hello);
        assert result != null;
        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());
    }
}
