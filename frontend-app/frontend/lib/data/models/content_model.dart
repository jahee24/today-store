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
      requestId: (json['requestId'] ?? '').toString(),
      taskId: (json['taskId'] ?? '').toString(),
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
  bool get isError => status == 'error';

  factory ContentTaskResponse.fromJson(Map<String, dynamic> json) {
    return ContentTaskResponse(
      taskId: (json['task_id'] ?? json['taskId'] ?? '').toString(),
      status: (json['status'] ?? '').toString(),
      result: json['result']?.toString(),
      errorMessage: json['error_message']?.toString(),
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

int? _asInt(dynamic value) {
  if (value is int) {
    return value;
  }
  if (value is String) {
    return int.tryParse(value);
  }
  return null;
}
