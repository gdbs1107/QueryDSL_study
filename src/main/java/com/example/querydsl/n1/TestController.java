package com.example.querydsl.n1;

import com.example.querydsl.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final MemberRepository memberRepository;

    @GetMapping("/all-members")
    public List<TestDTO> getAllMembers() {
        List<Member> members = memberRepository.findAll();

        return members.stream()
                .map(member -> TestDTO.builder()
                        .memberName(member.getUsername())
                        .teamName(member.getTeam().getName())
                        .build())
                .collect(Collectors.toList());
    }

    @GetMapping("/all-members2")
    public List<TestDTO> getAllMembers2() {
        List<Member> members = memberRepository.findAllWithTeam();

        return members.stream()
                .map(member -> TestDTO.builder()
                        .memberName(member.getUsername())
                        .teamName(member.getTeam().getName())
                        .build())
                .collect(Collectors.toList());
    }

}
