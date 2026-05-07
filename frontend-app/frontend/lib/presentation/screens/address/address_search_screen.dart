import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/datasources/remote/naver_local_api.dart';
import '../../models/address_pick_result.dart';
import '../../models/address_search_item.dart';

class AddressSearchScreen extends StatefulWidget {
  const AddressSearchScreen({super.key});

  @override
  State<AddressSearchScreen> createState() => _AddressSearchScreenState();
}

class _AddressSearchScreenState extends State<AddressSearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocusNode = FocusNode();

  late final NaverLocalApi _naverLocalApi;

  Timer? _debounce;
  bool _isLoading = false;
  String? _errorText;
  List<AddressSearchItem> _results = [];

  @override
  void initState() {
    super.initState();
    _naverLocalApi = NaverLocalApi(
      clientId: AppKeys.naverMapClientId,
      clientSecret: AppKeys.naverMapClientSecret,
    );
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    _searchFocusNode.dispose();
    super.dispose();
  }

  void _onChanged(String value) {
    _debounce?.cancel();

    final trimmed = value.trim();

    setState(() {
      _errorText = null;
    });

    if (trimmed.isEmpty) {
      setState(() {
        _isLoading = false;
        _results = [];
        _errorText = null;
      });
      return;
    }

    _debounce = Timer(const Duration(milliseconds: 350), () {
      _search(trimmed);
    });
  }

  Future<void> _search(String query) async {
    final trimmed = query.trim();
    if (trimmed.isEmpty) {

      setState(() {
        _isLoading = false;
        _results = [];
        _errorText = null;
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _errorText = null;
    });

    try {
      if (AppKeys.naverMapClientId.isEmpty ||
          AppKeys.naverMapClientSecret.isEmpty) {
        throw Exception('Naver API key is missing');
      }

      final result = await _naverLocalApi.searchAddress(trimmed);

      if (!mounted) return;

      setState(() {
        _results = result;
        _isLoading = false;
        _errorText = result.isEmpty ? '검색 결과가 없습니다.' : null;
      });
    } catch (e) {
      if (!mounted) return;

      var message = '주소 검색 중 오류가 발생했습니다.';
      final raw = e.toString();
      if (raw.contains('401')) {
        message = '네이버 지도 인증에 실패했습니다. API 키를 확인해주세요.';
      } else if (raw.contains('429')) {
        message = '주소 검색 요청이 많습니다. 잠시 후 다시 시도해주세요.';
      } else if (raw.contains('Naver API key is missing')) {
        message = '네이버 지도 API 키가 설정되지 않았습니다.';
      }

      setState(() {
        _results = [];
        _isLoading = false;
        _errorText = message;
      });

      debugPrint('주소 검색 에러: $e');
    }
  }

  Future<void> _goToMapPicker() async {
    _searchFocusNode.unfocus();

    final result = await context.push<AddressPickResult>(
      '/map-location-picker',
    );

    if (!mounted || result == null) return;
    context.pop(result);
  }

  void _selectAddress(AddressSearchItem item) {
    final result = AddressPickResult(
      roadAddress:
          item.roadAddress.isNotEmpty ? item.roadAddress : item.addressName,
      jibunAddress:
          item.jibunAddress.isNotEmpty ? item.jibunAddress : null,
      latitude: item.latitude,
      longitude: item.longitude,
    );

    context.pop(result);
  }

  void _clearSearch() {
    _debounce?.cancel();
    _searchController.clear();

    setState(() {
      _results = [];
      _isLoading = false;
      _errorText = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final hasText = _searchController.text.trim().isNotEmpty;
    final showGuide = !hasText && !_isLoading && _results.isEmpty;

    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: SafeArea(
        child: GestureDetector(
          onTap: () => _searchFocusNode.unfocus(),
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: h(20)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(height: h(10)),

                Stack(
                  alignment: Alignment.center,
                  children: [
                    Row(
                      children: [
                        IconButton(
                          onPressed: () => context.pop(),
                          icon: const Icon(
                            Icons.arrow_back_ios_new_rounded,
                            color: AppTheme.textPrimary,
                          ),
                        ),
                      ],
                    ),
                    const Center(
                      child: Text(
                        '주소 검색',
                        style: TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.textPrimary,
                        ),
                      ),
                    ),
                  ],
                ),
                SizedBox(height: h(30)),
                const Text(
                  '가게 주소를\n검색해주세요',
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w800,
                    color: AppTheme.textPrimary,
                    height: 1.3,
                  ),
                ),
                SizedBox(height: h(22)),
                Container(
                  decoration: BoxDecoration(
                    color: const Color(0xFFF1F1F5),
                    borderRadius: BorderRadius.circular(22),
                  ),
                  child: TextField(
                    controller: _searchController,
                    focusNode: _searchFocusNode,
                    onChanged: _onChanged,
                    textInputAction: TextInputAction.search,
                    onSubmitted: _search,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                    ),
                    decoration: InputDecoration(
                      hintText: '지번, 도로명, 건물명으로 검색',
                      hintStyle: const TextStyle(
                        fontSize: 18,
                        color: AppTheme.textTertiary,
                        fontWeight: FontWeight.w500,
                      ),
                      prefixIcon: const Padding(
                        padding: EdgeInsets.only(left: 8),
                        child: Icon(
                          Icons.search,
                          size: 34,
                          color: AppTheme.textTertiary,
                        ),
                      ),
                      prefixIconConstraints: const BoxConstraints(
                        minWidth: 54,
                        minHeight: 54,
                      ),
                      suffixIcon: hasText
                          ? IconButton(
                              onPressed: _clearSearch,
                              icon: const Icon(
                                Icons.cancel,
                                color: Color(0xFFB5B5BC),
                                size: 34,
                              ),
                            )
                          : null,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(22),
                        borderSide: BorderSide.none,
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(22),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(22),
                        borderSide: const BorderSide(
                          color: AppTheme.primaryColor,
                          width: 1.4,
                        ),
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 18,
                        vertical: 20,
                      ),
                    ),
                  ),
                ),
                SizedBox(height: h(14)),
                
                InkWell(
                  onTap: _goToMapPicker,
                  borderRadius: BorderRadius.circular(18),
                  child: Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: const Color(0xFFD8D8DE),
                        width: 1.4,
                      ),
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.my_location_rounded,
                          color: AppTheme.textPrimary,
                          size: 28,
                        ),
                        SizedBox(width: 10),
                        Text(
                          '현재 위치로 찾기',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                            color: AppTheme.textPrimary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                SizedBox(height: h(20)),
                if (showGuide) ...[
                  const Text(
                    '이렇게 검색해 보세요',
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.w800,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                  SizedBox(height: h(8)),
                  const Text(
                    '• 도로명 + 건물번호 (위례성대로 2)\n'
                    '• 건물명 + 번지 (방이동 44-2)\n'
                    '• 건물명, 아파트명 (반포 자이, 분당 주공 1차)',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: AppTheme.textTertiary,
                      height: 1.8,
                    ),
                  ),
                ] else ...[
                  Expanded(
                    child: Builder(
                      builder: (context) {
                        if (_isLoading) {
                          return const Center(
                            child: CircularProgressIndicator(),
                          );
                        }

                        if (_errorText != null) {
                          return Center(
                            child: Text(
                              _errorText!,
                              style: const TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: AppTheme.textTertiary,
                              ),
                            ),
                          );
                        }

                        return ListView.separated(
                          padding: const EdgeInsets.only(top: 4, bottom: 24),
                          itemCount: _results.length,
                          separatorBuilder: (_, __) => const Divider(
                            height: 1,
                            thickness: 1,
                            color: Color(0xFFF0F0F3),
                          ),
                          itemBuilder: (context, index) {
                            final item = _results[index];

                            return InkWell(
                              onTap: () => _selectAddress(item),
                              child: Padding(
                                padding:
                                    const EdgeInsets.symmetric(vertical: 18),
                                child: Row(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.start,
                                  children: [
                                    const Padding(
                                      padding: EdgeInsets.only(top: 2),
                                      child: Icon(
                                        Icons.location_on_outlined,
                                        size: 28,
                                        color: AppTheme.textPrimary,
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            item.roadAddress.isNotEmpty
                                                ? item.roadAddress
                                                : item.addressName,
                                            style: const TextStyle(
                                              fontSize: 18,
                                              fontWeight: FontWeight.w800,
                                              color: AppTheme.textPrimary,
                                              height: 1.35,
                                            ),
                                          ),
                                          if (item.jibunAddress.isNotEmpty) ...[
                                            const SizedBox(height: 8),
                                            Text(
                                              item.jibunAddress,
                                              style: const TextStyle(
                                                fontSize: 14,
                                                fontWeight: FontWeight.w500,
                                                color: AppTheme.textTertiary,
                                                height: 1.45,
                                              ),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        );
                      },
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}