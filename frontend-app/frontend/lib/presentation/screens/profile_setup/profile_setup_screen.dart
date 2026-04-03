import 'package:flutter/material.dart';
import '../../../config/app_theme.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/buttons/primary_button.dart';

class ProfileSetupScreen extends StatefulWidget {
  const ProfileSetupScreen({super.key});

  @override
  State<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends State<ProfileSetupScreen> {
  final TextEditingController _storeNameController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  final FocusNode _storeNameFocusNode = FocusNode();
  final FocusNode _addressFocusNode = FocusNode();

  String? selectedBusinessType;
  String? selectedStyle;

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
  bool get _isStyleFilled => selectedStyle != null;

  bool get _isStoreNameError => _showValidation && !_isStoreNameFilled;
  bool get _isBusinessTypeError => _showValidation && !_isBusinessTypeFilled;
  bool get _isAddressError => _showValidation && !_isAddressFilled;

  int get _currentStepCount {
    int count = 0;
    if (_isStoreNameFilled) count++;
    if (_isBusinessTypeFilled) count++;
    if (_isAddressFilled) count++;
    if (_isStyleFilled) count++;

    return count;
  }

  bool get _canGoNext {
    return _isStoreNameFilled && _isBusinessTypeFilled && _isAddressFilled;
  }

  void _handleNext() {
    setState(() {
      _showValidation = true;
    });

    if (!_canGoNext) return;

    context.go('/dashboard');
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            children: [
              const SizedBox(height: 16),
              Row(
                children: [
                  Text(
                    '가게 등록',
                    style: textTheme.headlineMedium?.copyWith(
                      fontSize: 23,
                      fontWeight: FontWeight.w700,
                      color: AppTheme.textPrimary,
                      letterSpacing: 0.2,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    '$_currentStepCount/4',
                    style: textTheme.titleMedium?.copyWith(
                      fontSize: 17,
                      fontWeight: FontWeight.w500,
                      color: AppTheme.textTertiary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  _buildStepBar(isActive: _isStoreNameFilled),
                  const SizedBox(width: 8),
                  _buildStepBar(isActive: _isBusinessTypeFilled),
                  const SizedBox(width: 8),
                  _buildStepBar(isActive: _isAddressFilled),
                  const SizedBox(width: 8),
                  _buildStepBar(isActive: _isStyleFilled),
                ],
              ),
              const SizedBox(height: 30),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '우리 가게 정보를\n알려주세요 🏪',
                        style: textTheme.headlineLarge?.copyWith(
                          fontSize: 29,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.textPrimary,
                          height: 1.3,
                          letterSpacing: 0.3
                        ),
                      ),

                      const SizedBox(height: 26),
                      _buildSectionLabel(
                        title: '상호명 *',
                        showError: _isStoreNameError,
                      ),

                      const SizedBox(height: 7),
                      _buildTextField(
                        controller: _storeNameController,
                        focusNode: _storeNameFocusNode,
                        hintText: '예) 맛있는 카페',
                        hasError: _isStoreNameError,
                      ),
                      const SizedBox(height: 26),

                      _buildSectionLabel(
                        title: '업종 *',
                        showError: _isBusinessTypeError,
                      ),
                      const SizedBox(height: 7),
                      _buildBusinessTypeWrap(),
                      const SizedBox(height: 26),

                      _buildSectionLabel(
                        title: '주소 *',
                        showError: _isAddressError,
                      ),
                      const SizedBox(height: 7),
                      _buildTextField(
                        controller: _addressController,
                        focusNode: _addressFocusNode,
                        hintText: '주소 검색',
                        hasError: _isAddressError,
                      ),
                      const SizedBox(height: 26),

                      _buildSectionLabel(
                        title: '선호 스타일',
                        showError: false,
                      ),
                      const SizedBox(height: 7),
                      Wrap(
                        spacing: 12,
                        runSpacing: 12,
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
                      const SizedBox(height: 6),

                      Text(
                        '콘텐츠의 기본 분위기를 선택해주세요',
                        style: textTheme.bodyMedium?.copyWith(
                          fontSize: 14,
                          color: AppTheme.textTertiary,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              PrimaryButton(
                text: '다음으로',
                onPressed: _handleNext,
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStepBar({required bool isActive}) {
    return Expanded(
      child: Container(
        height: 5,
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
            '필수 항목을 채워주세요',
            style: textTheme.bodyMedium?.copyWith(
              fontSize: 12,
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