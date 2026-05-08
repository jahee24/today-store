import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import '../../../config/constants.dart';

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
        final h = (double v) => AppLayout.h(dialogContext, v);
        final f = (double v) => AppLayout.f(dialogContext, v);
        return Dialog(
          backgroundColor: Colors.transparent,
          insetPadding: EdgeInsets.symmetric(horizontal: h(24)),
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFF2F2F2F),
              borderRadius: BorderRadius.circular(h(20)),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Padding(
                  padding: EdgeInsets.fromLTRB(h(20), h(24), h(20), h(18)),
                  child: Column(
                    children: [
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: f(19),
                          fontWeight: FontWeight.w700,
                          color: Colors.white,
                          height: 1.35,
                        ),
                      ),
                      SizedBox(height: h(8)),
                      Text(
                        description,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: f(15),
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
                          borderRadius: BorderRadius.only(
                            bottomLeft: Radius.circular(h(20)),
                          ),
                          child: Padding(
                            padding: EdgeInsets.symmetric(vertical: h(16)),
                            child: Text(
                              '취소',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: f(17),
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
                          borderRadius: BorderRadius.only(
                            bottomRight: Radius.circular(h(20)),
                          ),
                          child: Padding(
                            padding: EdgeInsets.symmetric(vertical: h(16)),
                            child: Text(
                              '지금 설정하기',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: f(17),
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
