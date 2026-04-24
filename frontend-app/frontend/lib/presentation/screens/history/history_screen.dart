import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../widgets/cards/recent_content_card.dart';
import '../../widgets/inputs/search_input.dart';
import '../../widgets/navigation/bottom_nav_bar.dart';

enum _HistoryType { text, image }

class _HistoryItem {
  const _HistoryItem({
    required this.title,
    required this.meta,
    required this.preview,
    required this.type,
  });

  final String title;
  final String meta;
  final String preview;
  final _HistoryType type;
}

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  _HistoryType? _selectedType;
  String _query = '';

  static const List<_HistoryItem> _allItems = [
    _HistoryItem(
      title: '시즌 딸기 라떼 홍보',
      meta: '2026.02.10 · 감성적',
      preview: '오늘도 향기로운 하루를 선물해 드릴게요...',
      type: _HistoryType.text,
    ),
    _HistoryItem(
      title: '매장 인테리어 사진',
      meta: '2026.02.09 · 8장 생성',
      preview: '다양한 구도로 변환된 매장 사진',
      type: _HistoryType.image,
    ),
    _HistoryItem(
      title: '설날 특가 이벤트',
      meta: '2026.02.07 · 친근',
      preview: '새해 복 많이 받으세요! 설날 맞이 특별...',
      type: _HistoryType.text,
    ),
  ];

  List<_HistoryItem> get _filteredItems {
    final normalized = _query.trim().toLowerCase();
    return _allItems.where((item) {
      if (_selectedType != null && item.type != _selectedType) {
        return false;
      }
      if (normalized.isEmpty) return true;
      return item.title.toLowerCase().contains(normalized) ||
          item.meta.toLowerCase().contains(normalized) ||
          item.preview.toLowerCase().contains(normalized);
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
    final items = _filteredItems;

    return Scaffold(
      backgroundColor: AppTheme.backgroundColor,
      bottomNavigationBar: const AppBottomNavBar(currentIndex: 2),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 20, 24, 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '콘텐츠 이력',
                style: textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 16),
              SearchInput(
                hintText: '콘텐츠 검색...',
                onChanged: (value) => setState(() => _query = value),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  _FilterChip(
                    label: '전체',
                    selected: _selectedType == null,
                    onTap: () => _selectFilter(null),
                  ),
                  const SizedBox(width: 10),
                  _FilterChip(
                    label: '문구',
                    selected: _selectedType == _HistoryType.text,
                    onTap: () => _selectFilter(_HistoryType.text),
                  ),
                  const SizedBox(width: 10),
                  _FilterChip(
                    label: '이미지',
                    selected: _selectedType == _HistoryType.image,
                    onTap: () => _selectFilter(_HistoryType.image),
                  ),
                ],
              ),
              const SizedBox(height: 18),
              Expanded(
                child: items.isEmpty
                    ? Center(
                        child: Text(
                          '검색 결과가 없어요.',
                          style: textTheme.bodyLarge?.copyWith(
                            color: AppTheme.textTertiary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      )
                    : ListView.separated(
                        itemCount: items.length,
                        separatorBuilder: (context, index) =>
                            const SizedBox(height: 14),
                        itemBuilder: (context, index) {
                          final item = items[index];
                          final isImage = item.type == _HistoryType.image;

                          return RecentContentCard(
                            title: item.title,
                            subtitle: '${item.meta}\n${item.preview}',
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
                      ),
              ),
            ],
          ),
        ),
      ),
    );
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
