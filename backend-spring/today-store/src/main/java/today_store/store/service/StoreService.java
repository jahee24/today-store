package today_store.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.store.dto.*;
import today_store.store.entity.PreferredStyle;
import today_store.store.entity.Store;
import today_store.store.exception.StoreAlreadyExistException;
import today_store.store.exception.StoreNotFoundException;
import today_store.store.repository.StoreRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateStoreResponse createStore(User user, CreateStoreRequest request) {
        if (storeRepository.existsByUser(user)) {
            throw new StoreAlreadyExistException();
        }

        Store store = Store.builder()
                .user(user)
                .storeName(request.getStoreName())
                .businessType(request.getBusinessType())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .preferredStyle(request.getPreferredStyle() != null ? request.getPreferredStyle() : PreferredStyle.CLEAN)
                .snsInstagram(request.getSns() != null ? request.getSns().getInstagram() : null)
                .snsNaverUrl(request.getSns() != null ? request.getSns().getNaver() : null)
                .snsKarrotUrl(request.getSns() != null ? request.getSns().getKarrot() : null)
                .createdAt(LocalDateTime.now())
                .build();

        Store savedStore = storeRepository.save(store);
        return CreateStoreResponse.from(savedStore);
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(User user) {
        Store store = storeRepository.findByUser(user)
                .orElseThrow(StoreNotFoundException::new);

        return StoreResponse.from(store);
    }

    @Transactional
    public UpdateStoreResponse updateStore(String email, UpdateStoreRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Store store = storeRepository.findByUser(user)
                .orElseThrow(StoreNotFoundException::new);

        store.update(
                request.getStoreName(),
                request.getPreferredStyle(),
                request.getSnsInstagram(),
                request.getSnsNaverUrl(),
                request.getSnsKarrotUrl(),
                request.getBusinessType(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude()
        );

        storeRepository.saveAndFlush(store);

        return UpdateStoreResponse.from(store);
    }
}