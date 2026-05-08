import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/models/store_model.dart';
import '../../../data/providers/dashboard_provider.dart';
import '../../widgets/buttons/primary_button.dart';
import '../../../data/providers/store_provider.dart';
import '../../models/address_pick_result.dart';

class ProfileSetupScreen extends ConsumerStatefulWidget {
  const ProfileSetupScreen({super.key});

  @override
  ConsumerState<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends ConsumerState<ProfileSetupScreen> {
  final TextEditingController _storeNameController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  final FocusNode _storeNameFocusNode = FocusNode();
  final FocusNode _addressFocusNode = FocusNode();

  String? selectedBusinessType;
  String? selectedStyle;

  double? _latitude;
  double? _longitude;
  bool _didPrefillFromServer = false;
  StoreProfileModel? _originalStoreProfile;



  final List<String> businessTypes = ['음식점', '카페', '소매점', '미용', '기타'];
  final List<String> styleOptions = ['무난', '깔끔', '친근', 'meme'];

  bool _showValidation = false;

  @override
  void initState() {
    super.initState();
    _storeNameController.addListener(_refresh);
    _addressController.addListener(_refresh);
    _storeNameFocusNode.addListener(_refresh);
    _addressFocusNode.addListener(_refresh);
  }

  @override
  void dispose() {
    _storeNameController.removeListener(_refresh);
    _addressController.removeListener(_refresh);
    _storeNameFocusNode.removeListener(_refresh);
    _addressFocusNode.removeListener(_refresh);

    _storeNameController.dispose();
    _addressController.dispose();
    _storeNameFocusNode.dispose();
    _addressFocusNode.dispose();
    super.dispose();
  }

  void _refresh() {
    setState(() {});
  }

  bool get _isStoreNameFilled => _storeNameController.text.trim().isNotEmpty;
  bool get _isBusinessTypeFilled => selectedBusinessType != null;
  bool get _isAddressFilled => _addressController.text.trim().isNotEmpty;
  bool get _isAddressMeaningful {
    final trimmed = _addressController.text.trim();
    if (trimmed.isEmpty) return false;
    const invalidAddressPlaceholders = {
      '선택한 위치',
      '주소를 불러오는 중...',
    };
    return !invalidAddressPlaceholders.contains(trimmed);
  }
  bool get _isLocationFilled => _latitude != null && _longitude != null;
  bool get _isStyleFilled => selectedStyle != null;

  bool get _isStoreNameError => _showValidation && !_isStoreNameFilled;
  bool get _isBusinessTypeError => _showValidation && !_isBusinessTypeFilled;
  bool get _isAddressError =>
      _showValidation &&
      (!_isAddressFilled || !_isAddressMeaningful || !_isLocationFilled);

  int get _currentStepCount {
    int count = 0;
    if (_isStoreNameFilled) count++;
    if (_isBusinessTypeFilled) count++;
    if (_isAddressFilled && _isLocationFilled) count++;
    if (_isStyleFilled) count++;

    return count;
  }

  bool get _canGoNext {
    return _isStoreNameFilled &&
        _isBusinessTypeFilled &&
        _isAddressFilled &&
        _isAddressMeaningful &&
        _isLocationFilled;
  }

  void _prefillFromStore(StoreProfileModel store) {
    if (_didPrefillFromServer) {
      return;
    }

    _didPrefillFromServer = true;
    _originalStoreProfile = store;
    _storeNameController.text = store.storeName;
    _addressController.text = store.address;
    _latitude = store.latitude;
    _longitude = store.longitude;
    selectedBusinessType =
        businessTypes.contains(store.businessType) ? store.businessType : null;
    selectedStyle = _mapPreferredStyleToLabel(store.preferredStyle);
    setState(() {});
  }

  String? _mapPreferredStyleToLabel(String? preferredStyle) {
    switch (preferredStyle) {
      case 'clean':
        return '깔끔';
      case 'friendly':
        return '친근';
      case 'emotional':
        return '무난';
      default:
        return null;
    }
  }

  Future<void> _handleAddressSearch() async {
    _addressFocusNode.unfocus();

    final result = await context.push<AddressPickResult>('/address-search');

    if (result == null) return;

    setState(() {
      _addressController.text = result.displayAddress;
      _latitude = result.latitude;
      _longitude = result.longitude;
    });
  }

  void _handleNext() {
    setState(() {
      _showValidation = true;
    });

    if (!_canGoNext) return;

    final isEditMode = GoRouterState.of(context).uri.queryParameters['mode'] == 'edit';
    final notifier = ref.read(storeProvider.notifier);
    if (isEditMode) {
      final original = _originalStoreProfile;
      if (original == null) {
        // Forced dashboard entry can send first-time users into edit mode.
        // In that case, persist the form as a new store profile.
        notifier.createStore(
          storeName: _storeNameController.text.trim(),
          businessType: selectedBusinessType!,
          address: _addressController.text.trim(),
          latitude: _latitude!,
          longitude: _longitude!,
          preferredStyleLabel: selectedStyle,
        );
        return;
      }
      final trimmedStoreName = _storeNameController.text.trim();
      final trimmedAddress = _addressController.text.trim();
      final changedStoreName =
          trimmedStoreName != original.storeName.trim()
              ? trimmedStoreName
              : null;
      final changedBusinessType =
          selectedBusinessType != original.businessType
              ? selectedBusinessType
              : null;
      final changedAddress =
          trimmedAddress != original.address.trim()
              ? trimmedAddress
              : null;
      final changedLatitude = _latitude != null && _latitude != original.latitude
          ? _latitude
          : null;
      final changedLongitude = _longitude != null && _longitude != original.longitude
          ? _longitude
          : null;
      final currentPreferredStyleApi = _toPreferredStyleApiValue(selectedStyle);
      final changedPreferredStyle =
          currentPreferredStyleApi != original.preferredStyle
              ? selectedStyle
              : null;

      final hasChanges =
          changedStoreName != null ||
          changedBusinessType != null ||
          changedAddress != null ||
          changedLatitude != null ||
          changedLongitude != null ||
          changedPreferredStyle != null;

      if (!hasChanges) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('변경된 내용이 없어요.')));
        return;
      }

      notifier.updateStore(
        storeName: changedStoreName,
        businessType: changedBusinessType,
        address: changedAddress,
        latitude: changedLatitude,
        longitude: changedLongitude,
        preferredStyleLabel: changedPreferredStyle,
      );
      return;
    }

    notifier.createStore(
      storeName: _storeNameController.text,
      businessType: selectedBusinessType!,
      address: _addressController.text,
      latitude: _latitude!,
      longitude: _longitude!,
      preferredStyleLabel: selectedStyle,
    );
  }

  String? _toPreferredStyleApiValue(String? styleLabel) {
    switch (styleLabel) {
      case '깔끔':
        return 'clean';
      case '친근':
        return 'friendly';
      case '무난':
        return 'emotional';
      default:
        return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final storeState = ref.watch(storeProvider);
    final isEditMode = GoRouterState.of(context).uri.queryParameters['mode'] == 'edit';
    final storeProfileAsync = isEditMode ? ref.watch(currentStoreProfileProvider) : null;

    if (isEditMode) {
      storeProfileAsync!.whenData((store) {
        if (store == null || _didPrefillFromServer) {
          return;
        }
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted || _didPrefillFromServer) {
            return;
          }
          _prefillFromStore(store);
        });
      });
    }

    ref.listen<StoreState>(storeProvider, (previous, next) {
      if (next.errorMessage != null && next.errorMessage != previous?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }

      if (next.createdStore != null && previous?.createdStore == null) {
        ref.invalidate(currentStoreProfileProvider);
        ref.invalidate(dashboardDataProvider);
        context.go('/dashboard');
      }

      if (next.updatedStore != null && previous?.updatedStore == null) {
        ref.invalidate(currentStoreProfileProvider);
        ref.invalidate(dashboardDataProvider);
        context.go('/dashboard');
      }
    });

    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.symmetric(horizontal: h(20)),
          child: Column(
            children: [
              SizedBox(height: h(14)),
              Row(
                children: [
                  if (isEditMode) ...[
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      style: IconButton.styleFrom(
                        backgroundColor: AppTheme.fillLight,
                      ),
                      icon: const Icon(Icons.arrow_back_rounded),
                    ),
                    SizedBox(width: h(8)),
                  ],
                  Text(
                    isEditMode ? '가게 정보 수정' : '가게 등록',
                    style: textTheme.headlineSmall?.copyWith(
                      fontSize: f(23),
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                      letterSpacing: 0,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    '$_currentStepCount/4',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: f(17),
                      fontWeight: FontWeight.w500,
                      color: AppTheme.textTertiary,
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(14)),
              Row(
                children: [
                  _buildStepBar(isActive: _isStoreNameFilled),
                  SizedBox(width: h(8)),
                  _buildStepBar(isActive: _isBusinessTypeFilled),
                  SizedBox(width: h(8)),
                  _buildStepBar(isActive: _isAddressFilled && _isLocationFilled),
                  SizedBox(width: h(8)),
                  _buildStepBar(isActive: _isStyleFilled),
                ],
              ),
              SizedBox(height: h(24)),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '우리 가게 정보를\n알려주세요 🏪',
                        style: textTheme.headlineLarge?.copyWith(
                          fontSize: f(29),
                          fontWeight: FontWeight.w700,
                          color: AppTheme.textPrimary,
                          height: 1.3,
                          letterSpacing: 0.3
                        ),
                      ),

                      SizedBox(height: h(22)),
                      _buildSectionLabel(
                        title: '상호명 *',
                        showError: _isStoreNameError,
                      ),

                      SizedBox(height: h(6)),
                      _buildTextField(
                        controller: _storeNameController,
                        focusNode: _storeNameFocusNode,
                        hintText: '예) 맛있는 카페',
                        hasError: _isStoreNameError,
                      ),
                      SizedBox(height: h(22)),

                      _buildSectionLabel(
                        title: '업종 *',
                        showError: _isBusinessTypeError,
                      ),
                      SizedBox(height: h(6)),
                      _buildBusinessTypeWrap(),
                      SizedBox(height: h(22)),

                      _buildSectionLabel(
                        title: '주소 *',
                        showError: _isAddressError,
                      ),
                      SizedBox(height: h(6)),
                      _buildTextField(
                        controller: _addressController,
                        focusNode: _addressFocusNode,
                        hintText: '주소 검색',
                        hasError: _isAddressError,
                        readOnly: true,
                        onTap: _handleAddressSearch,
                      ),
                      SizedBox(height: h(22)),

                      _buildSectionLabel(
                        title: '선호 스타일',
                        showError: false,
                      ),
                      SizedBox(height: h(6)),
                      Wrap(
                        spacing: h(10),
                        runSpacing: h(10),
                        children: styleOptions.map((style) {
                          final isSelected = selectedStyle == style;
                          return _buildChoiceChip(
                            label: style,
                            isSelected: isSelected,
                            onTap: () {
                              setState(() {
                                selectedStyle = selectedStyle == style ? null : style;
                              });
                            },
                          );
                        }).toList(),
                      ),
                      SizedBox(height: h(6)),

                      Text(
                        '콘텐츠의 기본 분위기를 선택해주세요',
                        style: textTheme.bodyMedium?.copyWith(
                          fontSize: f(14),
                          color: AppTheme.textTertiary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: h(18)),

              PrimaryButton(
                text: isEditMode ? '저장' : '다음으로',
                onPressed: storeState.isLoading ? null : _handleNext,
                isLoading: storeState.isLoading,
              ),
              if (isEditMode && storeProfileAsync!.isLoading)
                Padding(
                  padding: EdgeInsets.only(top: h(8)),
                  child: Text(
                    '기존 가게 정보를 불러오는 중이에요...',
                    style: TextStyle(
                      fontSize: f(13),
                      color: AppTheme.textTertiary,
                    ),
                  ),
                ),
              SizedBox(height: h(14)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStepBar({required bool isActive}) {
    return Expanded(
      child: Container(
        height: AppLayout.h(context, 5),
        decoration: BoxDecoration(
          color: isActive ? AppTheme.primaryColor : AppTheme.progressInactive,
          borderRadius: BorderRadius.circular(999),
        ),
      ),
    );
  }

  Widget _buildSectionLabel ({
    required String title,
    required bool showError,
  }) {
    final textTheme = Theme.of(context).textTheme;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(
          title,
          style: textTheme.titleMedium?.copyWith(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: AppTheme.textSecondary,
          ),
        ),
        if (showError) ...[
          const SizedBox(width: 8),
          Text(
            title.startsWith('주소')
                ? '지도에서 정확한 주소를 선택해주세요'
                : '필수 항목을 채워주세요',
            style: textTheme.bodyMedium?.copyWith(
              fontSize: AppLayout.f(context, 12),
              fontWeight: FontWeight.w600,
              color: AppTheme.dangerText,
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required FocusNode focusNode,
    required String hintText,
    required bool hasError,
    bool readOnly = false,
    VoidCallback? onTap,
  }) {
    final bool isFocused = focusNode.hasFocus;
    final bool hasValue = controller.text.trim().isNotEmpty;

    final Color normalBorderColor =
    hasError ? AppTheme.dangerText : AppTheme.borderStrongColor;
    final Color activeBorderColor =
    hasError ? AppTheme.dangerText : AppTheme.borderStrongColor;

    return TextField(
      controller: controller,
      focusNode: focusNode,
      readOnly: readOnly,
      onTap: onTap,
      style: const TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.w700,
        color: AppTheme.textPrimary,
      ),
      decoration: InputDecoration(
        hintText: hintText,
        hintStyle: const TextStyle(
          fontSize: 18,
          color: AppTheme.textSecondary,
          fontWeight: FontWeight.w400,
        ),
        filled: true,
        fillColor: Color(0xFFFAFAFA),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 19,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(
            color: (isFocused || hasValue) ? activeBorderColor : normalBorderColor,
            width: hasError ? 1.6 : 1.4,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(
            color: AppTheme.primaryColor,
            width: 1.8,
          ),
        ),
      ),
    );
  }

  Widget _buildBusinessTypeWrap() {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        border: _isBusinessTypeError
        ? Border.all(
          color: AppTheme.dangerText,
          width: 1.4,
        )
        : null,
      ),
      padding: _isBusinessTypeError
      ? const EdgeInsets.all(10)
      : EdgeInsets.zero,
      child: Wrap(
        spacing: 12,
        runSpacing: 12,
        children: businessTypes.map((type) {
          final isSelected = selectedBusinessType == type;
          return _buildChoiceChip(
            label: type, 
            isSelected: isSelected, 
            onTap: (){
              setState(() {
                selectedBusinessType = type;
              });
            },
          );
        }).toList(),
      ),
    );
  }

  Widget _buildChoiceChip({
    required String label,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 160),
        curve: Curves.easeOut,
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected
          ? AppTheme.infoBg
          : AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color:
                isSelected ? AppTheme.primaryColor : AppTheme.borderStrongColor,
            width: 1.8,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 16,
            fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
            color: isSelected ? AppTheme.primaryColor : AppTheme.textPrimary,
          ),
        ),
      ),
    );
  }
}