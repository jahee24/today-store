import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'network_provider.dart';
import '../datasources/remote/content_api.dart';
import '../models/content_model.dart';
import '../models/store_model.dart';
import '../repositories/content_repository.dart';
import 'store_provider.dart';

class DashboardStats {
  final int totalCreated;
  final int totalShared;
  final int createdThisWeek;

  const DashboardStats({
    required this.totalCreated,
    required this.totalShared,
    required this.createdThisWeek,
  });
}

class DashboardData {
  final StoreProfileModel? storeProfile;
  final DashboardStats stats;
  final List<ContentRequestItem> recentRequests;

  const DashboardData({
    required this.storeProfile,
    required this.stats,
    required this.recentRequests,
  });
}

final contentApiProvider = Provider<ContentApi>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ContentApi(apiClient.dio);
});

final contentRepositoryProvider = Provider<ContentRepository>((ref) {
  final contentApi = ref.watch(contentApiProvider);
  return ContentRepository(contentApi: contentApi);
});

final contentDetailProvider =
    FutureProvider.autoDispose.family<ContentDetail, String>((ref, contentId) async {
  final trimmed = contentId.trim();
  if (trimmed.isEmpty) {
    throw Exception('콘텐츠 ID가 없어요.');
  }
  final repository = ref.read(contentRepositoryProvider);
  return repository.getContent(contentId: trimmed);
});

final dashboardDataProvider = FutureProvider<DashboardData>((ref) async {
  final contentRepository = ref.read(contentRepositoryProvider);
  final storeRepository = ref.read(storeRepositoryProvider);

  StoreProfileModel? storeProfile;
  try {
    storeProfile = await storeRepository.getMyStore();
  } catch (_) {
    storeProfile = null;
  }

  ContentRequestsResponse requestsResponse;
  try {
    requestsResponse = await contentRepository.getContentRequests(page: 1, size: 30);
  } catch (_) {
    requestsResponse = ContentRequestsResponse(
      data: const [],
      pagination: ContentRequestsPagination(
        currentPage: 1,
        pageSize: 30,
        totalCount: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      ),
    );
  }
  final requests = requestsResponse.data;

  final now = DateTime.now();
  final startOfWeek = now.subtract(Duration(days: now.weekday - 1));
  final weekStart = DateTime(
    startOfWeek.year,
    startOfWeek.month,
    startOfWeek.day,
  );

  final createdThisWeek = requests.where((item) {
    final created = item.createdAt;
    if (created == null) {
      return false;
    }
    return created.isAfter(weekStart) || created.isAtSameMomentAs(weekStart);
  }).length;

  int totalShared = 0;
  final idsToCheck = requests.take(10).map((e) => e.requestId).where((e) => e.isNotEmpty).toList();
  for (final id in idsToCheck) {
    try {
      final res = await contentRepository.getRequestContents(requestId: id);
      totalShared += res.contents.where((item) => item.isPosted).length;
    } catch (_) {}
  }

  return DashboardData(
    storeProfile: storeProfile,
    stats: DashboardStats(
      totalCreated: requestsResponse.pagination.totalCount,
      totalShared: totalShared,
      createdThisWeek: createdThisWeek,
    ),
    recentRequests: requests.take(2).toList(),
  );
});
