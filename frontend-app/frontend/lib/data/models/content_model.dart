class ContentRequestItem {
  final String requestId;
  final String concept;
  final String? thumbnailUrl;
  final int imageCount;
  final DateTime? createdAt;

  const ContentRequestItem({
    required this.requestId,
    required this.concept,
    this.thumbnailUrl,
    required this.imageCount,
    this.createdAt,
  });

  factory ContentRequestItem.fromJson(Map<String, dynamic> json) {
    return ContentRequestItem(
      requestId: (json['requestId'] ?? '').toString(),
      concept: (json['concept'] ?? '').toString(),
      thumbnailUrl: json['thumbnailUrl']?.toString(),
      imageCount: _asInt(json['imageCount']) ?? 0,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

class ContentCreateResponse {
  final String requestId;
  final DateTime? createdAt;

  const ContentCreateResponse({
    required this.requestId,
    this.createdAt,
  });

  factory ContentCreateResponse.fromJson(Map<String, dynamic> json) {
    return ContentCreateResponse(
      requestId: (json['requestId'] ?? '').toString(),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

class ContentGenerateResponse {
  final String requestId;
  final String taskId;
  final DateTime? startedAt;

  const ContentGenerateResponse({
    required this.requestId,
    required this.taskId,
    this.startedAt,
  });

  factory ContentGenerateResponse.fromJson(Map<String, dynamic> json) {
    return ContentGenerateResponse(
      requestId: (json['requestId'] ?? json['request_id'] ?? '').toString(),
      taskId: (json['taskId'] ?? json['task_id'] ?? '').toString(),
      startedAt: json['startedAt'] != null
          ? DateTime.tryParse(json['startedAt'].toString())
          : null,
    );
  }
}

class ContentTaskResponse {
  final String taskId;
  final String status;
  final String? result;
  final String? errorMessage;

  const ContentTaskResponse({
    required this.taskId,
    required this.status,
    this.result,
    this.errorMessage,
  });

  bool get isProcessing => status == 'processing';

  bool get isSuccess => status == 'success';

  bool get isError => status == 'error' || status == 'timeout';

  factory ContentTaskResponse.fromJson(Map<String, dynamic> json) {
    final rawStatus = (json['status'] ?? '').toString().trim().toLowerCase();
    return ContentTaskResponse(
      taskId: (json['task_id'] ?? json['taskId'] ?? '').toString(),
      status: rawStatus,
      result: json['result']?.toString(),
      errorMessage: (json['error_message'] ?? json['errorMessage'])?.toString(),
    );
  }
}

class ContentRequestsPagination {
  final int currentPage;
  final int pageSize;
  final int totalCount;
  final int totalPages;
  final bool hasNext;
  final bool hasPrevious;

  const ContentRequestsPagination({
    required this.currentPage,
    required this.pageSize,
    required this.totalCount,
    required this.totalPages,
    required this.hasNext,
    required this.hasPrevious,
  });

  factory ContentRequestsPagination.fromJson(Map<String, dynamic> json) {
    return ContentRequestsPagination(
      currentPage: _asInt(json['currentPage']) ?? 1,
      pageSize: _asInt(json['pageSize']) ?? 10,
      totalCount: _asInt(json['totalCount']) ?? 0,
      totalPages: _asInt(json['totalPages']) ?? 0,
      hasNext: json['hasNext'] as bool? ?? false,
      hasPrevious: json['hasPrevious'] as bool? ?? false,
    );
  }
}

class ContentRequestsResponse {
  final List<ContentRequestItem> data;
  final ContentRequestsPagination pagination;

  const ContentRequestsResponse({
    required this.data,
    required this.pagination,
  });

  factory ContentRequestsResponse.fromJson(Map<String, dynamic> json) {
    final list = (json['data'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(ContentRequestItem.fromJson)
        .toList();
    final paginationJson = json['pagination'] as Map<String, dynamic>? ?? const {};
    return ContentRequestsResponse(
      data: list,
      pagination: ContentRequestsPagination.fromJson(paginationJson),
    );
  }
}

class GeneratedContentItem {
  final String contentId;
  final bool isPosted;
  final DateTime? createdAt;

  const GeneratedContentItem({
    required this.contentId,
    required this.isPosted,
    this.createdAt,
  });

  factory GeneratedContentItem.fromJson(Map<String, dynamic> json) {
    return GeneratedContentItem(
      contentId: (json['contentId'] ?? '').toString(),
      isPosted: json['isPosted'] as bool? ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

class RequestContentsResponse {
  final String requestId;
  final List<GeneratedContentItem> contents;

  const RequestContentsResponse({
    required this.requestId,
    required this.contents,
  });

  factory RequestContentsResponse.fromJson(Map<String, dynamic> json) {
    final list = (json['contents'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(GeneratedContentItem.fromJson)
        .toList();
    return RequestContentsResponse(
      requestId: (json['requestId'] ?? '').toString(),
      contents: list,
    );
  }
}

class GenerationRequestDetailResponse {
  final String id;
  final List<GenerationRequestImage> images;

  const GenerationRequestDetailResponse({
    required this.id,
    required this.images,
  });

  factory GenerationRequestDetailResponse.fromJson(Map<String, dynamic> json) {
    final rawImages = (json['images'] as List<dynamic>? ?? const []);
    return GenerationRequestDetailResponse(
      id: (json['id'] ?? '').toString(),
      images: rawImages
          .whereType<Map<String, dynamic>>()
          .map(GenerationRequestImage.fromJson)
          .toList(),
    );
  }
}

class GenerationRequestImage {
  final String id;
  final String url;

  const GenerationRequestImage({
    required this.id,
    required this.url,
  });

  factory GenerationRequestImage.fromJson(Map<String, dynamic> json) {
    return GenerationRequestImage(
      id: (json['id'] ?? '').toString(),
      url: (json['url'] ?? '').toString(),
    );
  }
}

class ImageVariationStartResponse {
  final String requestId;
  final String taskId;
  final DateTime? startedAt;

  const ImageVariationStartResponse({
    required this.requestId,
    required this.taskId,
    this.startedAt,
  });

  factory ImageVariationStartResponse.fromJson(Map<String, dynamic> json) {
    return ImageVariationStartResponse(
      requestId: (json['requestId'] ?? '').toString(),
      taskId: (json['taskId'] ?? '').toString(),
      startedAt: json['startedAt'] != null
          ? DateTime.tryParse(json['startedAt'].toString())
          : null,
    );
  }
}

class ImageVariationItem {
  final String id;
  final String url;
  final String angleType;

  const ImageVariationItem({
    required this.id,
    required this.url,
    required this.angleType,
  });

  factory ImageVariationItem.fromJson(Map<String, dynamic> json) {
    return ImageVariationItem(
      id: (json['id'] ?? '').toString(),
      url: (json['url'] ?? '').toString(),
      angleType: (json['angleType'] ?? '').toString(),
    );
  }
}

/// `GET /api/v1/contents/{contentId}` 응답.
class ContentDetail {
  final String id;
  final String requestId;
  final String generationType;
  final bool isPosted;
  final DateTime? createdAt;
  final ContentDetailData contentData;
  final List<ContentDetailImage> images;

  const ContentDetail({
    required this.id,
    required this.requestId,
    required this.generationType,
    required this.isPosted,
    this.createdAt,
    required this.contentData,
    required this.images,
  });

  factory ContentDetail.fromJson(Map<String, dynamic> json) {
    final dataJson = json['contentData'] as Map<String, dynamic>? ?? const {};
    return ContentDetail(
      id: (json['id'] ?? '').toString(),
      requestId: (json['requestId'] ?? '').toString(),
      generationType: (json['generationType'] ?? '').toString(),
      isPosted: json['isPosted'] as bool? ?? false,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      contentData: ContentDetailData.fromJson(dataJson),
      images: (json['images'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ContentDetailImage.fromJson)
          .toList(),
    );
  }
}

class ContentDetailData {
  final ContentPlatformCopy instagram;
  final ContentPlatformCopy karrot;
  final ContentPlatformCopy naver;

  const ContentDetailData({
    required this.instagram,
    required this.karrot,
    required this.naver,
  });

  factory ContentDetailData.fromJson(Map<String, dynamic> json) {
    return ContentDetailData(
      instagram: ContentPlatformCopy.fromJson(
        json['instagram'] as Map<String, dynamic>? ?? const {},
      ),
      karrot: ContentPlatformCopy.fromJson(
        json['karrot'] as Map<String, dynamic>? ?? const {},
      ),
      naver: ContentPlatformCopy.fromJson(
        json['naver'] as Map<String, dynamic>? ?? const {},
      ),
    );
  }
}

class ContentPlatformCopy {
  final String text;
  final List<String> tagList;

  const ContentPlatformCopy({
    required this.text,
    required this.tagList,
  });

  factory ContentPlatformCopy.fromJson(Map<String, dynamic> json) {
    final raw = json['hashtags'] ?? json['tags'] ?? json['keywords'];
    List<String> list = const [];
    if (raw is List) {
      list = raw.map((e) => e.toString()).toList();
    }
    return ContentPlatformCopy(
      text: (json['text'] ?? '').toString(),
      tagList: list,
    );
  }

  String get hashtagsLine {
    if (tagList.isEmpty) return '';
    return tagList
        .map((t) {
          final s = t.trim();
          if (s.isEmpty) return '';
          return s.startsWith('#') ? s : '#$s';
        })
        .where((s) => s.isNotEmpty)
        .join(' ');
  }
}

class ContentDetailImage {
  final String id;
  final String inputImageId;
  final String url;
  final DateTime? createdAt;

  const ContentDetailImage({
    required this.id,
    required this.inputImageId,
    required this.url,
    this.createdAt,
  });

  factory ContentDetailImage.fromJson(Map<String, dynamic> json) {
    return ContentDetailImage(
      id: (json['id'] ?? '').toString(),
      inputImageId: (json['inputImageId'] ?? '').toString(),
      url: (json['url'] ?? '').toString(),
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
    );
  }
}

int? _asInt(dynamic value) {
  if (value is int) {
    return value;
  }
  if (value is String) {
    return int.tryParse(value);
  }
  return null;
}
