package com.example.resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ResourceServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceServerApplication.class, args);
	}

	/**
	 * 개발/데모용 시드 데이터.
	 * TodoRepository는 인메모리 Map이라 재시작마다 비므로, 부팅 직후 몇 건을 넣어둔다.
	 * 운영 환경에선 빼거나 Spring Profile(@Profile("dev"))로 격리하는 게 좋다.
	 */
	@Bean
	CommandLineRunner seedTodos(TodoRepository repo) {
		return args -> {
			repo.add("MCP 데모 준비", "01-personal 강의 자료 정리");
			repo.add("우유 사기", "저지방으로");
			Todo done = repo.add("리드미 작성", "사용법 섹션 보강");
			repo.complete(done.id()); // 완료된 상태도 한 건 만들어 둠 → 필터 테스트용
			repo.add("운동 가기", null); // memo 비어있는 케이스
		};
	}
}
