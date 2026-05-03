import '../datasources/remote/content_api.dart';
import '../models/content_model.dart';

class ContentRepository {
  final ContentApi contentApi;

  ContentRepository({
    required this.contentApi,
  });

  Future<ContentRequestsResponse> getContentRequests({
    int page = 1,
    int size = 10,
  }) {
    return contentApi.getContentRequests(page: page, size: size);
  }

  Future<RequestContentsResponse> getRequestContents({
    required String requestId,
  }) {
    return contentApi.getRequestContents(requestId: requestId);
  }

  Future<ContentCreateResponse> createContentRequest({
    required String concept,
    String? additionalNote,
    String? targetAge,
    String? targetGender,
    required List<String> imageDescriptions,
    required List<String> imagePaths,
  }) {
    return contentApi.createContentRequest(
      concept: concept,
      additionalNote: additionalNote,
      targetAge: targetAge,
      targetGender: targetGender,
      imageDescriptions: imageDescriptions,
      imagePaths: imagePaths,
    );
  }

  Future<ContentGenerateResponse> generateContent({
    required String requestId,
  }) {
    return contentApi.generateContent(requestId: requestId);
  }

  Future<ContentTaskResponse> getTaskStatus({
    required String apiLogId,
  }) {
    return contentApi.getTaskStatus(apiLogId: apiLogId);
  }

  Future<ContentDetail> getContent({
    required String contentId,
  }) {
    return contentApi.getContent(contentId: contentId);
  }
}
