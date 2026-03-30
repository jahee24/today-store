package today_store.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import today_store.authentication.entity.User;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.store.dto.CreateStoreRequest;
import today_store.store.dto.CreateStoreResponse;
import today_store.store.dto.SnsInfo;
import today_store.store.dto.StoreResponse;
import today_store.store.dto.UpdateStoreRequest;
import today_store.store.dto.UpdateStoreResponse;
import today_store.store.entity.PreferredStyle;
import today_store.store.entity.Store;
import today_store.store.exception.StoreAlreadyExistException;
import today_store.store.exception.StoreNotFoundException;
import today_store.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("가게 서비스 테스트")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository, userRepository);
    }

    @Test
    @DisplayName("가게 생성 기본 스타일 적용")
    void shouldCreateStoreWithDefaultPreferredStyle() {
        // 선호 스타일이 없으면 CLEAN 기본값으로 가게를 생성해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        UUID storeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 12, 0);
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeName("오늘상점")
                .businessType("소품샵")
                .address("서울시 마포구")
                .latitude(new BigDecimal("37.55555555"))
                .longitude(new BigDecimal("126.12345678"))
                .build();
        given(storeRepository.existsByUser(user)).willReturn(false);
        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            ReflectionTestUtils.setField(store, "id", storeId);
            ReflectionTestUtils.setField(store, "createdAt", createdAt);
            return store;
        });

        // when
        CreateStoreResponse response = storeService.createStore(user, request);

        // then
        assertThat(response.getId()).isEqualTo(storeId);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        then(storeRepository).should().save(argThat(store ->
                store.getUser() == user
                        && store.getStoreName().equals("오늘상점")
                        && store.getBusinessType().equals("소품샵")
                        && store.getAddress().equals("서울시 마포구")
                        && store.getLatitude().compareTo(new BigDecimal("37.55555555")) == 0
                        && store.getLongitude().compareTo(new BigDecimal("126.12345678")) == 0
                        && store.getPreferredStyle() == PreferredStyle.CLEAN
                        && store.getSnsInstagram() == null
                        && store.getSnsNaverUrl() == null
                        && store.getSnsKarrotUrl() == null
        ));
    }

    @Test
    @DisplayName("가게 생성 요청값 반영")
    void shouldCreateStoreWithProvidedPreferredStyleAndSns() {
        // 요청에 들어온 선호 스타일과 SNS 정보를 그대로 저장해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        UUID storeId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 30, 12, 10);
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeName("오늘카페")
                .businessType("카페")
                .address("서울시 성동구")
                .latitude(new BigDecimal("37.12345678"))
                .longitude(new BigDecimal("127.87654321"))
                .preferredStyle(PreferredStyle.FRIENDLY)
                .sns(SnsInfo.builder()
                        .instagram("@todaycafe")
                        .naver("https://naver.me/todaycafe")
                        .karrot("https://www.daangn.com/todaycafe")
                        .build())
                .build();
        given(storeRepository.existsByUser(user)).willReturn(false);
        given(storeRepository.save(any(Store.class))).willAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            ReflectionTestUtils.setField(store, "id", storeId);
            ReflectionTestUtils.setField(store, "createdAt", createdAt);
            return store;
        });

        // when
        CreateStoreResponse response = storeService.createStore(user, request);

        // then
        assertThat(response.getId()).isEqualTo(storeId);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        then(storeRepository).should().save(argThat(store ->
                store.getPreferredStyle() == PreferredStyle.FRIENDLY
                        && store.getSnsInstagram().equals("@todaycafe")
                        && store.getSnsNaverUrl().equals("https://naver.me/todaycafe")
                        && store.getSnsKarrotUrl().equals("https://www.daangn.com/todaycafe")
        ));
    }

    @Test
    @DisplayName("중복 가게 생성 실패")
    void shouldThrowWhenStoreAlreadyExists() {
        // 이미 가게가 등록된 사용자라면 중복 생성 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        CreateStoreRequest request = CreateStoreRequest.builder()
                .storeName("오늘상점")
                .businessType("소품샵")
                .address("서울시 마포구")
                .latitude(new BigDecimal("37.55555555"))
                .longitude(new BigDecimal("126.12345678"))
                .build();
        given(storeRepository.existsByUser(user)).willReturn(true);

        // when
        StoreAlreadyExistException exception = assertThrows(
                StoreAlreadyExistException.class,
                () -> storeService.createStore(user, request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_ALREADY_EXISTS);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.STORE_ALREADY_EXISTS.getMessage());
        then(storeRepository).should(never()).save(any(Store.class));
    }

    @Test
    @DisplayName("가게 프로필 응답 매핑")
    void shouldMapStoreToStoreResponse() {
        // 가게 엔티티를 조회 응답 DTO로 변환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        Store store = createStore(user);
        given(storeRepository.findByUser(user)).willReturn(Optional.of(store));

        // when
        StoreResponse response = storeService.getStore(user);

        // then
        assertThat(response.getId()).isEqualTo(store.getId());
        assertThat(response.getStoreName()).isEqualTo("오늘카페");
        assertThat(response.getBusinessType()).isEqualTo("카페");
        assertThat(response.getAddress()).isEqualTo("서울시 성동구");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.12345678");
        assertThat(response.getLongitude()).isEqualByComparingTo("127.87654321");
        assertThat(response.getPreferredStyle()).isEqualTo(PreferredStyle.FRIENDLY);
        assertThat(response.getSns().getInstagram()).isEqualTo("@todaycafe");
        assertThat(response.getSns().getNaver()).isEqualTo("https://naver.me/todaycafe");
        assertThat(response.getSns().getKarrot()).isEqualTo("https://www.daangn.com/todaycafe");
    }

    @Test
    @DisplayName("존재하지 않는 가게 조회 실패")
    void shouldThrowWhenStoreDoesNotExist() {
        // 사용자의 가게가 없으면 가게 없음 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        given(storeRepository.findByUser(user)).willReturn(Optional.empty());

        // when
        StoreNotFoundException exception = assertThrows(
                StoreNotFoundException.class,
                () -> storeService.getStore(user)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.STORE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("가게 프로필 수정")
    void shouldUpdateStoreProfile() {
        // 요청에 포함된 필드만 변경하고 수정 응답을 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        Store store = createStore(user);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 3, 30, 13, 15);
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .storeName("오늘카페 리뉴얼")
                .preferredStyle(PreferredStyle.MEME)
                .snsInstagram("@renewedcafe")
                .address("서울시 송파구")
                .latitude(new BigDecimal("37.00000000"))
                .longitude(new BigDecimal("128.00000000"))
                .build();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(storeRepository.findByUser(user)).willReturn(Optional.of(store));
        given(storeRepository.saveAndFlush(store)).willAnswer(invocation -> {
            ReflectionTestUtils.setField(store, "updatedAt", updatedAt);
            return store;
        });

        // when
        UpdateStoreResponse response = storeService.updateStore("user@example.com", request);

        // then
        assertThat(store.getStoreName()).isEqualTo("오늘카페 리뉴얼");
        assertThat(store.getPreferredStyle()).isEqualTo(PreferredStyle.MEME);
        assertThat(store.getSnsInstagram()).isEqualTo("@renewedcafe");
        assertThat(store.getSnsNaverUrl()).isEqualTo("https://naver.me/todaycafe");
        assertThat(store.getBusinessType()).isEqualTo("카페");
        assertThat(store.getAddress()).isEqualTo("서울시 송파구");
        assertThat(store.getLatitude()).isEqualByComparingTo("37.00000000");
        assertThat(store.getLongitude()).isEqualByComparingTo("128.00000000");
        assertThat(response.getId()).isEqualTo(store.getId());
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        then(storeRepository).should().saveAndFlush(store);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 가게 수정 실패")
    void shouldThrowWhenUpdatingStoreForMissingUser() {
        // 수정 요청 이메일에 해당하는 사용자가 없으면 사용자 없음 예외를 반환해야 한다.

        // given
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .storeName("오늘카페 리뉴얼")
                .build();
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> storeService.updateStore("missing@example.com", request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 가게 수정 실패")
    void shouldThrowWhenUpdatingMissingStore() {
        // 사용자는 있지만 가게가 없으면 가게 없음 예외를 반환해야 한다.

        // given
        User user = createUser("user@example.com", "사장님");
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .storeName("오늘카페 리뉴얼")
                .build();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(storeRepository.findByUser(user)).willReturn(Optional.empty());

        // when
        StoreNotFoundException exception = assertThrows(
                StoreNotFoundException.class,
                () -> storeService.updateStore("user@example.com", request)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STORE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.STORE_NOT_FOUND.getMessage());
    }

    private User createUser(String email, String name) {
        User user = new User(email, name, "google", "provider-id", "https://image.test/profile.png");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Store createStore(User user) {
        Store store = Store.builder()
                .user(user)
                .storeName("오늘카페")
                .businessType("카페")
                .address("서울시 성동구")
                .latitude(new BigDecimal("37.12345678"))
                .longitude(new BigDecimal("127.87654321"))
                .preferredStyle(PreferredStyle.FRIENDLY)
                .snsInstagram("@todaycafe")
                .snsNaverUrl("https://naver.me/todaycafe")
                .snsKarrotUrl("https://www.daangn.com/todaycafe")
                .build();
        ReflectionTestUtils.setField(store, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(store, "updatedAt", LocalDateTime.of(2026, 3, 30, 12, 30));
        return store;
    }
}
