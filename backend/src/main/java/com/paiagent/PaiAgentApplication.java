package com.paiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.paiagent.mapper")
public class PaiAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaiAgentApplication.class, args);
	}

}
