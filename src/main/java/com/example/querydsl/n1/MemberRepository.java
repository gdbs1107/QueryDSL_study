package com.example.querydsl.n1;

import com.example.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member findByUsername(String username);

    @Query("select m from Member m join fetch m.team")
    List<Member> findAllWithTeam();
}
