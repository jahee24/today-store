enum StyleType {
  plain,
  clean,
  friendly,
  meme,
}

extension StyleTypeX on StyleType {
  String get apiValue {
    switch (this) {
      case StyleType.plain:
        return 'PLAIN';
      case StyleType.clean:
        return 'CLEAN';
      case StyleType.friendly:
        return 'FRIENDLY';
      case StyleType.meme:
        return 'MEME';
    }
  }

  String get label {
    switch (this) {
      case StyleType.plain:
        return '무난';
      case StyleType.clean:
        return '깔끔';
      case StyleType.friendly:
        return '친근';
      case StyleType.meme:
        return 'meme';
    }
  }

  static StyleType? fromLabel(String? label) {
    switch (label) {
      case '무난':
        return StyleType.plain;
      case '깔끔':
        return StyleType.clean;
      case '친근':
        return StyleType.friendly;
      case 'meme':
        return StyleType.meme;
      default:
        return null;
    }
  }
}