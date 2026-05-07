import 'package:flutter/material.dart';
import '../../../config/constants.dart';

// 사용자에게 한 번 더 확인받는 팝업
class AppConfirmDialog extends StatelessWidget {
  final String title;
  final String content;
  final String cancelText;
  final String confirmText;
  final VoidCallback onConfirm;

  const AppConfirmDialog({
    super.key,
    required this.title,
    required this.content,
    required this.onConfirm,
    this.cancelText = '취소',
    this.confirmText = '확인',
  });

  @override
  Widget build(BuildContext context) {
    final h = (double v) => AppLayout.h(context, v);
    final f = (double v) => AppLayout.f(context, v);
    return AlertDialog(
      titlePadding: EdgeInsets.fromLTRB(h(20), h(18), h(10), 0),
      contentPadding: EdgeInsets.fromLTRB(h(20), h(10), h(20), 0),
      actionsPadding: EdgeInsets.fromLTRB(h(20), h(14), h(20), h(18)),
      title: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: TextStyle(fontSize: f(18), fontWeight: FontWeight.w700),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(cancelText, style: TextStyle(fontSize: f(14))),
          ),
        ],
      ),
      content: Text(content, style: TextStyle(fontSize: f(14))),
      actions: [
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
            onConfirm();
          },
          child: Text(confirmText, style: TextStyle(fontSize: f(14))),
        ),
      ],
    );
  }
}