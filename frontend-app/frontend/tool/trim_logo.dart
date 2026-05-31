import 'dart:io';
import 'dart:math' as math;

import 'package:image/image.dart' as img;

/// 로고 variants 생성 (스플래시 / Android 12 branding / 로그인).
void main() {
  const inputPath = 'assets/images/app_logo_large.png';
  const splashOut = 'assets/images/app_logo_splash.png';
  const loginOut = 'assets/images/app_logo_login.png';
  const iconOut = 'assets/images/app_icon_splash.png';
  const brandingOut = 'assets/images/app_logo_branding.png';
  const blankOut = 'assets/images/splash_blank.png';

  final bytes = File(inputPath).readAsBytesSync();
  final source = img.decodePng(bytes);
  if (source == null) {
    stderr.writeln('Failed to decode $inputPath');
    exit(1);
  }

  final bounds = _contentBounds(source);
  final cropped = img.copyCrop(
    source,
    x: bounds.left,
    y: bounds.top,
    width: bounds.width,
    height: bounds.height,
  );

  // Android 11 이하: 전체 로고 (원형 아님)
  final splashPad = (cropped.width * 0.08).round();
  final splash = _padImage(cropped, splashPad);
  File(splashOut).writeAsBytesSync(img.encodePng(splash));

  // 로그인
  final loginPad = (cropped.width * 0.04).round();
  final login = _padImage(cropped, loginPad);
  File(loginOut).writeAsBytesSync(img.encodePng(login));

  // Android 12+ 중앙: 일러스트만 (원 안에 들어가도록)
  final iconHeight = (cropped.height * 0.58).round();
  final iconCrop = img.copyCrop(
    cropped,
    x: 0,
    y: 0,
    width: cropped.width,
    height: iconHeight,
  );
  final iconSquare = _toSquare(iconCrop, (iconCrop.width * 0.1).round());
  File(iconOut).writeAsBytesSync(img.encodePng(iconSquare));

  // Android 12+ 하단 branding: 800x320 (원형 아님, 글씨 포함 전체 로고)
  final branding = img.Image(width: 800, height: 320);
  img.fill(branding, color: img.ColorRgb8(255, 255, 255));
  final maxW = 760;
  final maxH = 280;
  final scale = math.min(maxW / cropped.width, maxH / cropped.height);
  final targetW = (cropped.width * scale).round();
  final targetH = (cropped.height * scale).round();
  final scaled = img.copyResize(cropped, width: targetW, height: targetH);
  img.compositeImage(
    branding,
    scaled,
    dstX: (800 - targetW) ~/ 2,
    dstY: (320 - targetH) ~/ 2,
  );
  File(brandingOut).writeAsBytesSync(img.encodePng(branding));

  // Android 12 중앙 아이콘 비우기 (런처 아이콘 대신)
  final blank = img.Image(width: 1, height: 1);
  img.fill(blank, color: img.ColorRgb8(255, 255, 255));
  File(blankOut).writeAsBytesSync(img.encodePng(blank));

  stdout.writeln('Content: ${bounds.width}x${bounds.height}');
  stdout.writeln('Splash (API<=30): ${splash.width}x${splash.height}');
  stdout.writeln('Icon (API31+ center): ${iconSquare.width}x${iconSquare.height}');
  stdout.writeln('Branding (API31+): 800x320');
  stdout.writeln('Login: ${login.width}x${login.height}');
}

img.Image _padImage(img.Image content, int pad) {
  final canvas = img.Image(
    width: content.width + pad * 2,
    height: content.height + pad * 2,
  );
  img.fill(canvas, color: img.ColorRgb8(255, 255, 255));
  img.compositeImage(canvas, content, dstX: pad, dstY: pad);
  return canvas;
}

img.Image _toSquare(img.Image content, int pad) {
  final side = math.max(content.width, content.height) + pad * 2;
  final square = img.Image(width: side, height: side);
  img.fill(square, color: img.ColorRgb8(255, 255, 255));
  img.compositeImage(
    square,
    content,
    dstX: (side - content.width) ~/ 2,
    dstY: (side - content.height) ~/ 2,
  );
  return square;
}

class _Bounds {
  _Bounds(this.left, this.top, this.width, this.height);
  final int left;
  final int top;
  final int width;
  final int height;
}

_Bounds _contentBounds(img.Image image) {
  var minX = image.width;
  var minY = image.height;
  var maxX = 0;
  var maxY = 0;

  for (var y = 0; y < image.height; y++) {
    for (var x = 0; x < image.width; x++) {
      if (_isContentPixel(image.getPixel(x, y))) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
      }
    }
  }

  return _Bounds(minX, minY, maxX - minX + 1, maxY - minY + 1);
}

bool _isContentPixel(img.Pixel pixel) {
  final a = pixel.a.toInt();
  if (a < 8) return false;

  final r = pixel.r.toInt();
  final g = pixel.g.toInt();
  final b = pixel.b.toInt();

  if (r > 240 && g > 240 && b > 240) return false;

  final isBrandBlue = b > r + 12 && b > g + 4 && b > 80;
  final isDarkText = r < 80 && g < 80 && b < 120;
  final isMidBlue = b > 100 && r < 120 && g < 150;

  return isBrandBlue || isDarkText || isMidBlue;
}
