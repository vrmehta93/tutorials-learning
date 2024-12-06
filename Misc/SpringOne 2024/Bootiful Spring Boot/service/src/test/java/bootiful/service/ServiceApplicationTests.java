package bootiful.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class ServiceApplicationTests {

	@Test
	void contextLoads() {
		var appModules = ApplicationModules.of(ServiceApplication.class);
		appModules.verify();
		System.out.println(appModules.toString());

		new Documenter(appModules).writeDocumentation();
	}

}
