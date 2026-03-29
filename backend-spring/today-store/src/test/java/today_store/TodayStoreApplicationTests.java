package today_store;

import com.google.cloud.storage.Storage;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TodayStoreApplicationTests {

	@MockitoBean
	private Storage storage;

	@MockitoBean
	private ProxyManager<String> proxyManager;

	@Test
	void contextLoads() {
	}

}
