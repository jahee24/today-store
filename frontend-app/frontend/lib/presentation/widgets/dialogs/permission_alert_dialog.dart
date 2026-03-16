import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

class PermissionAlertDialog {
  PermissionAlertDialog._();

  static Future<void> show(
    BuildContext context, {
    required String title,
    required String description,
  }) async {
    await showDialog<void>(
      context: context,
      barrierDismissible: true,
      builder: (dialogContext) {
        return Dialog(
          backgroundColor: Colors.transparent,
          insetPadding: const EdgeInsets.symmetric(horizontal: 28),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFF2F2F2F),
              borderRadius: BorderRadius.circular(22),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 28, 24, 20),
                  child: Column(
                    children: [
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 19,
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                          height: 1.35,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        description,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                          color: Color(0xFFE3E3E3),
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
                const Divider(
                  height: 1,
                  thickness: 0.8,
                  color: Color(0xFF4A4A4A),
                ),
                IntrinsicHeight(
                  child: Row(
                    children: [
                      Expanded(
                        child: InkWell(
                          onTap: () => Navigator.of(dialogContext).pop(),
                          borderRadius: const BorderRadius.only(
                            bottomLeft: Radius.circular(22),
                          ),
                          child: const Padding(
                            padding: EdgeInsets.symmetric(vertical: 18),
                            child: Text(
                              '취소',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF4D8DFF),
                              ),
                            ),
                          ),
                        ),
                      ),
                      const VerticalDivider(
                        width: 1,
                        thickness: 0.8,
                        color: Color(0xFF4A4A4A),
                      ),
                      Expanded(
                        child: InkWell(
                          onTap: () async {
                            Navigator.of(dialogContext).pop();
                            await openAppSettings();
                          },
                          borderRadius: const BorderRadius.only(
                            bottomRight: Radius.circular(22),
                          ),
                          child: const Padding(
                            padding: EdgeInsets.symmetric(vertical: 18),
                            child: Text(
                              '지금 설정하기',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF4D8DFF),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
