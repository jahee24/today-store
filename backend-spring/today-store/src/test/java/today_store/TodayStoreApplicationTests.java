package today_store;

import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TodayStoreApplicationTests {

	@MockitoBean
	private Storage storage;

	@Test
	void contextLoads() {
	}

}
