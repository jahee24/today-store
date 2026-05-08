import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/content_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../widgets/cards/recent_content_card.dart';
import '../../widgets/inputs/search_input.dart';
import '../../widgets/navigation/bottom_nav_bar.dart';

enum _HistoryType { text, image }

final historyRequestsProvider = FutureProvider<ContentRequestsResponse>((ref) {
  final repository = ref.read(contentRepositoryProvider);
  return repository.getContentRequests(page: 1, size: 30);
});

class _HistoryItem {
  const _HistoryItem({
    required this.title,
    required this.meta,
    required this.type,
  });

  final String title;
  final String meta;
  final _HistoryType type;
}

class HistoryScreen extends ConsumerStatefulWidget {
  const HistoryScreen({super.key});

  @override
  ConsumerState<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends ConsumerState<HistoryScreen> {
  _HistoryType? _selectedType;
  String _query = '';

  List<_HistoryItem> _buildFilteredItems(List<ContentRequestItem> requests) {
    final items = requests.map((request) {
      final isImage = request.imageCount > 0;
      final type = isImage ? _HistoryType.image : _HistoryType.text;
      final title = request.concept.isNotEmpty ? request.concept : '제목 없음';
      final meta = isImage
          ? '${_formatRelative(request.createdAt)} · ${request.imageCount}장 생성'
          : '${_formatRelative(request.createdAt)} · 문구';
      return _HistoryItem(
        title: title,
        meta: meta,
        type: type,
      );
    }).toList();

    final normalized = _query.trim().toLowerCase();
    return items.where((item) {
      if (_selectedType != null && item.type != _selectedType) {
        return false;
      }
      if (normalized.isEmpty) {
        return true;
      }
      return item.title.toLowerCase().contains(normalized) ||
          item.meta.toLowerCase().contains(normalized);
    }).toList();
  }

  void _openItem(_HistoryItem item) {
    if (item.type == _HistoryType.image) {
      context.push('/image-result');
      return;
    }
    context.push('/result');
  }

  void _selectFilter(_HistoryType? type) {
    setState(() => _selectedType = type);
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final historyAsync = ref.watch(historyRequestsProvider);
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      bottomNavigationBar: const AppBottomNavBar(currentIndex: 2),
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(h(20), h(16), h(20), h(14)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '콘텐츠 이력',
                style: textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  fontSize: f(32),
                ),
              ),
              SizedBox(height: h(14)),
              SearchInput(
                hintText: '콘텐츠 검색...',
                onChanged: (value) => setState(() => _query = value),
              ),
              SizedBox(height: h(14)),
              Row(
                children: [
                  _FilterChip(
                    label: '전체',
                    selected: _selectedType == null,
                    onTap: () => _selectFilter(null),
                  ),
                  SizedBox(width: h(8)),
                  _FilterChip(
                    label: '문구',
                    selected: _selectedType == _HistoryType.text,
                    onTap: () => _selectFilter(_HistoryType.text),
                  ),
                  SizedBox(width: h(8)),
                  _FilterChip(
                    label: '이미지',
                    selected: _selectedType == _HistoryType.image,
                    onTap: () => _selectFilter(_HistoryType.image),
                  ),
                ],
              ),
              SizedBox(height: h(14)),
              Expanded(
                child: historyAsync.when(
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (_, __) => Center(
                    child: Text(
                      '콘텐츠 이력을 불러오지 못했어요.',
                      style: textTheme.bodyLarge?.copyWith(
                        color: AppTheme.textTertiary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  data: (response) {
                    final requests = response.data;
                    final items = _buildFilteredItems(requests);
                    if (items.isEmpty) {
                      return Center(
                        child: Text(
                          requests.isEmpty ? '아직 생성된 콘텐츠가 없어요.' : '검색 결과가 없어요.',
                          style: textTheme.bodyLarge?.copyWith(
                            color: AppTheme.textTertiary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      );
                    }
                    return ListView.separated(
                      itemCount: items.length,
                      separatorBuilder: (context, index) => SizedBox(height: h(12)),
                      itemBuilder: (context, index) {
                        final item = items[index];
                        final isImage = item.type == _HistoryType.image;
                        return RecentContentCard(
                          title: item.title,
                          subtitle: item.meta,
                          badgeText: isImage ? '이미지' : '문구',
                          badgeTextColor: isImage
                              ? const Color(0xFF5B9B4C)
                              : AppTheme.primaryColor,
                          badgeBgColor: isImage
                              ? const Color(0xFFEAF6E5)
                              : const Color(0xFFEEEAFE),
                          thumbnailEmoji: isImage ? '🎨' : '📸',
                          thumbnailBgColor: isImage
                              ? const Color(0xFFE6F2F5)
                              : const Color(0xFFF7ECEA),
                          onTap: () => _openItem(item),
                        );
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatRelative(DateTime? createdAt) {
    if (createdAt == null) {
      return '날짜 없음';
    }
    final now = DateTime.now();
    final diff = now.difference(createdAt);
    if (diff.inMinutes < 1) {
      return '방금 전';
    }
    if (diff.inHours < 1) {
      return '${diff.inMinutes}분 전';
    }
    if (diff.inDays < 1) {
      return '${diff.inHours}시간 전';
    }
    if (diff.inDays == 1) {
      return '어제';
    }
    return '${createdAt.year}.${createdAt.month.toString().padLeft(2, '0')}.${createdAt.day.toString().padLeft(2, '0')}';
  }
}

class _FilterChip extends StatelessWidget {
  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFF5F3FF) : AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: selected ? AppTheme.primaryColor : AppTheme.borderStrongColor,
          ),
        ),
        child: Text(
          label,
          style: Theme.of(context).textTheme.titleSmall?.copyWith(
                color: selected ? AppTheme.primaryColor : AppTheme.textPrimary,
                fontWeight: FontWeight.w700,
              ),
        ),
      ),
    );
  }
}
