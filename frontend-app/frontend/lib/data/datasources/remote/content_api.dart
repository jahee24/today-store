import 'package:dio/dio.dart';

import '../../models/content_model.dart';

class ContentApi {
  final Dio dio;

  ContentApi(this.dio);

  Future<ContentRequestsResponse> getContentRequests({
    int page = 1,
    int size = 10,
  }) async {
    final response = await dio.get(
      '/api/v1/contents/requests',
      queryParameters: {
        'page': page,
        'size': size,
      },
    );

    return ContentRequestsResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<RequestContentsResponse> getRequestContents({
    required String requestId,
  }) async {
    final response = await dio.get('/api/v1/contents/$requestId/contents');
    return RequestContentsResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<ContentCreateResponse> createContentRequest({
    required String concept,
    String? additionalNote,
    String? targetAge,
    String? targetGender,
    required List<String> imageDescriptions,
    required List<String> imagePaths,
  }) async {
    final files = await Future.wait(
      imagePaths.map((path) async {
        return MultipartFile.fromFile(
          path,
          filename: path.split('\\').last,
        );
      }),
    );

    final formData = FormData.fromMap({
      'concept': concept,
      if (additionalNote != null && additionalNote.trim().isNotEmpty)
        'additionalNote': additionalNote.trim(),
      if (targetAge != null && targetAge.trim().isNotEmpty)
        'targetAge': targetAge.trim(),
      if (targetGender != null && targetGender.trim().isNotEmpty)
        'targetGender': targetGender.trim(),
      'imageDescriptions': imageDescriptions,
      'images': files,
    });

    final response = await dio.post('/api/v1/contents/request', data: formData);
    return ContentCreateResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<ContentGenerateResponse> generateContent({
    required String requestId,
  }) async {
    final response = await dio.post('/api/v1/contents/$requestId/generate');
    return ContentGenerateResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<ContentTaskResponse> getTaskStatus({
    required String apiLogId,
  }) async {
    final response = await dio.get('/api/v1/contents/task/$apiLogId');
    return ContentTaskResponse.fromJson(response.data as Map<String, dynamic>);
  }

  Future<ContentDetail> getContent({
    required String contentId,
  }) async {
    final response = await dio.get('/api/v1/contents/$contentId');
    return ContentDetail.fromJson(response.data as Map<String, dynamic>);
  }
}
