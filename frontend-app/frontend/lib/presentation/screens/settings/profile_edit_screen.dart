import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../config/app_theme.dart';
import '../../../config/constants.dart';
import '../../../data/providers/auth_provider.dart';
import '../../widgets/buttons/primary_button.dart';

class ProfileEditScreen extends ConsumerStatefulWidget {
  const ProfileEditScreen({super.key});

  @override
  ConsumerState<ProfileEditScreen> createState() => _ProfileEditScreenState();
}

class _ProfileEditScreenState extends ConsumerState<ProfileEditScreen> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  bool _didPrefill = false;
  bool _isSaving = false;
  String _initialName = '';
  String _initialEmail = '';

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  void _prefill(String name, String email) {
    if (_didPrefill) {
      return;
    }
    _didPrefill = true;
    _initialName = name;
    _initialEmail = email;
    _nameController.text = name;
    _emailController.text = email;
  }

  Future<void> _handleSave() async {
    if (_isSaving) {
      return;
    }

    final trimmedName = _nameController.text.trim();
    final trimmedEmail = _emailController.text.trim();
    if (trimmedName.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('이름을 입력해 주세요.')));
      return;
    }

    if (trimmedEmail.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('이메일을 입력해 주세요.')));
      return;
    }

    final emailPattern = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
    if (!emailPattern.hasMatch(trimmedEmail)) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('올바른 이메일 형식이 아니에요.')));
      return;
    }

    final hasNameChanged = trimmedName != _initialName.trim();
    final hasEmailChanged = trimmedEmail != _initialEmail.trim();
    if (!hasNameChanged && !hasEmailChanged) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('변경된 내용이 없어요.')));
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      final authRepository = ref.read(authRepositoryProvider);
      final updatedUser = await authRepository.updateMyProfile(
        name: hasNameChanged ? trimmedName : null,
        email: hasEmailChanged ? trimmedEmail : null,
      );
      _initialName = updatedUser.name;
      _initialEmail = updatedUser.email;
      _nameController.text = updatedUser.name;
      _emailController.text = updatedUser.email;

      ref.invalidate(currentUserProvider);

      if (!mounted) {
        return;
      }
      Navigator.of(context).pop();
    } catch (_) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('프로필 저장 중 오류가 발생했어요.')));
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    final currentUser = ref.watch(currentUserProvider);

    currentUser.whenData((user) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) {
          return;
        }
        _prefill(
          user.name.isNotEmpty ? user.name : '',
          user.email.isNotEmpty ? user.email : '',
        );
      });
    });

    return Scaffold(
      backgroundColor: AppTheme.surfaceColor,
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.fromLTRB(h(20), h(14), h(20), h(18)),
          child: Column(
            children: [
              Row(
                children: [
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: IconButton.styleFrom(
                      backgroundColor: AppTheme.fillLight,
                    ),
                    icon: const Icon(Icons.arrow_back_rounded),
                  ),
                  SizedBox(width: h(8)),
                  Expanded(
                    child: Text(
                      '프로필 수정',
                      style: textTheme.headlineSmall?.copyWith(
                        fontSize: f(23),
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0,
                        color: AppTheme.textPrimary,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(20)),
              Column(
                children: [
                  Container(
                    width: h(92),
                    height: h(92),
                    decoration: const BoxDecoration(
                      color: Color(0xFFF0EEFA),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.person_rounded,
                      size: h(52),
                      color: Color(0xFF5D4CB3),
                    ),
                  ),
                  SizedBox(height: h(8)),
                  Text(
                    '사진 변경',
                    style: textTheme.titleSmall?.copyWith(
                      color: AppTheme.primaryColor,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
              SizedBox(height: h(22)),
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _FieldLabel('이름'),
                      SizedBox(height: h(8)),
                      _ProfileTextField(
                        controller: _nameController,
                        hintText: '이름을 입력하세요',
                      ),
                      SizedBox(height: h(16)),
                      _FieldLabel('이메일'),
                      SizedBox(height: h(8)),
                      _ProfileTextField(
                        controller: _emailController,
                        hintText: '이메일',
                      ),
                      SizedBox(height: h(16)),
                      _FieldLabel('전화번호 (선택)'),
                      SizedBox(height: h(8)),
                      _ProfileTextField(
                        controller: _phoneController,
                        hintText: '010-0000-0000',
                        keyboardType: TextInputType.phone,
                      ),
                    ],
                  ),
                ),
              ),
              SizedBox(height: h(8)),
              PrimaryButton(
                text: '저장하기',
                onPressed: _isSaving ? null : _handleSave,
                isLoading: _isSaving,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _FieldLabel extends StatelessWidget {
  const _FieldLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: Theme.of(context).textTheme.titleMedium?.copyWith(
            color: AppTheme.textPrimary,
            fontWeight: FontWeight.w700,
          ),
    );
  }
}

class _ProfileTextField extends StatelessWidget {
  const _ProfileTextField({
    required this.controller,
    required this.hintText,
    this.keyboardType,
  });

  final TextEditingController controller;
  final String hintText;
  final TextInputType? keyboardType;

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
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
        fillColor: const Color(0xFFFAFAFA),
        contentPadding: EdgeInsets.symmetric(horizontal: h(16), vertical: h(16)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(h(18)),
          borderSide: const BorderSide(
            color: AppTheme.borderStrongColor,
            width: 1.4,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(h(18)),
          borderSide: const BorderSide(
            color: AppTheme.primaryColor,
            width: 1.8,
          ),
        ),
      ),
    );
  }
}
