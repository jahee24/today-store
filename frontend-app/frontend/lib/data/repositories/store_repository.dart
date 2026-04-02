import '../datasources/remote/store_api.dart';
import '../models/store_model.dart';
import '../models/style_type.dart';

class StoreRepository {
  final StoreApi storeApi;

  StoreRepository({
    required this.storeApi,
  });

  Future<StoreCreateResponse> createStore({
    required String storeName,
    required String businessType,
    required String address,
    required double latitude,
    required double longitude,
    String? preferredStyleLabel,
    String? instagram,
    String? naver,
    String? karrot,
  }) async {
    final styleType = StyleTypeX.fromLabel(preferredStyleLabel);

    final sns = StoreSnsModel(
      instagram: instagram,
      naver: naver,
      karrot: karrot,
    );

    final request = StoreCreateRequest(
      storeName: storeName, 
      businessType: businessType, 
      address: address, 
      latitude: latitude, 
      longitude: longitude,
      preferredStyle: styleType?.apiValue,
      sns: sns.isEmpty ? null : sns,
    );

    return storeApi.createStore(request);
  }
}